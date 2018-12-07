package io.norland.server.udp;

import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.norland.server.ScheduledEventLoop;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UdpIdleStateHandler extends ChannelDuplexHandler {
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long readerIdleTimeNanos;
    private final long writerIdleTimeNanos;
    private final long allIdleTimeNanos;

    private final ConcurrentHashMap<InetSocketAddress, UdpConnect> udpConnects
            = new ConcurrentHashMap<>();

    private final ScheduledEventLoop eventLoop = ScheduledEventLoop.getInstance();

    public UdpIdleStateHandler(
            long readerIdleTime, long writerIdleTime, long allIdleTime,
            TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (allIdleTime <= 0) {
            allIdleTimeNanos = 0;
        } else {
            allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            if (msg instanceof DatagramPacket) {
                DatagramPacket packet = (DatagramPacket) msg;
                InetSocketAddress socketAddress = packet.sender();
                UdpConnect udpConnect = udpConnects.get(socketAddress);
                if (udpConnect == null) {
                    udpConnect = newUdpConnect(ctx, socketAddress);
                    udpConnects.put(socketAddress, udpConnect);
                }
                udpConnect.setLastReadTime(System.nanoTime());
                udpConnect.setReading(true);//start reading
                ctx.fireChannelRead(msg);
                udpConnect.setReading(false);//read completed
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ChannelPromise channelPromise = promise;
        // Allow writing with void promise if handler is only configured for read timeout events.
        if (writerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            if (msg instanceof DatagramPacket) {
                DatagramPacket packet = (DatagramPacket) msg;
                InetSocketAddress socketAddress = packet.sender();
                UdpConnect udpConnect = udpConnects.get(socketAddress);
                if (udpConnect != null) {
                    channelPromise = promise.unvoid();
                    channelPromise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            udpConnect.setLastWriteTime(System.nanoTime());
                        }
                    });
                }
            }
        }
        ctx.write(msg, channelPromise);
    }

    private UdpConnect newUdpConnect(ChannelHandlerContext ctx,
                                     InetSocketAddress socketAddress) {
        UdpConnect udpConnect = new UdpConnect();
        long nanoTime = System.nanoTime();
        udpConnect.setLastReadTime(nanoTime);
        if (readerIdleTimeNanos > 0) {
            udpConnect.setReaderIdleTimeout(eventLoop.schedule(
                    new ReaderIdleTimeoutTask(ctx,
                            udpConnect,
                            socketAddress),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS));
        }
        if (writerIdleTimeNanos > 0) {
            udpConnect.setWriterIdleTimeout(eventLoop.schedule(
                    new WriterIdleTimeoutTask(ctx,
                            udpConnect,
                            socketAddress),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS));
        }
        if (allIdleTimeNanos > 0) {
            udpConnect.setAllIdleTimeout(eventLoop.schedule(
                    new AllIdleTimeoutTask(ctx,
                            udpConnect,
                            socketAddress),
                    readerIdleTimeNanos, TimeUnit.NANOSECONDS));
        }
        return udpConnect;
    }

    private void destroy() {
        for (Map.Entry<InetSocketAddress, UdpConnect> entry : udpConnects.entrySet()) {
            entry.getValue().destroy();
        }
        udpConnects.clear();
        eventLoop.shutdown();
    }

    protected void channelIdle(ChannelHandlerContext ctx, UdpIdleStateEvent evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    private final class ReaderIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;
        private final UdpConnect udpConnect;
        private final InetSocketAddress socketAddress;

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx,
                              UdpConnect udpConnect,
                              InetSocketAddress socketAddress) {
            this.ctx = ctx;
            this.udpConnect = udpConnect;
            this.socketAddress = socketAddress;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            long nextDelay = readerIdleTimeNanos;
            if (!udpConnect.isReading()) {
                nextDelay -= System.nanoTime() - udpConnect.getLastReadTime();
            }
            if (nextDelay <= 0) {
                try {
                    UdpIdleStateEvent
                            event = new UdpIdleStateEvent(IdleState.READER_IDLE,
                            socketAddress);
                    udpConnects.remove(socketAddress);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                udpConnect.setReaderIdleTimeout(
                        eventLoop.schedule(this,
                                readerIdleTimeNanos, TimeUnit.NANOSECONDS));
            }
        }
    }

    private final class WriterIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;
        private final UdpConnect udpConnect;
        private final InetSocketAddress socketAddress;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx,
                              UdpConnect udpConnect,
                              InetSocketAddress socketAddress) {
            this.ctx = ctx;
            this.udpConnect = udpConnect;
            this.socketAddress = socketAddress;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            long lastWriteTime = udpConnect.getLastWriteTime();
            long nextDelay = writerIdleTimeNanos - (System.nanoTime() - lastWriteTime);
            if (nextDelay <= 0) {
                try {
                    UdpIdleStateEvent event = new UdpIdleStateEvent(IdleState.WRITER_IDLE,
                            socketAddress);
                    udpConnects.remove(socketAddress);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                udpConnect.setWriterIdleTimeout(eventLoop.schedule(
                        this, writerIdleTimeNanos, TimeUnit.NANOSECONDS));
            }
        }
    }

    private final class AllIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;
        private final UdpConnect udpConnect;
        private final InetSocketAddress socketAddress;

        AllIdleTimeoutTask(ChannelHandlerContext ctx,
                           UdpConnect udpConnect,
                           InetSocketAddress socketAddress) {
            this.ctx = ctx;
            this.udpConnect = udpConnect;
            this.socketAddress = socketAddress;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            long nextDelay = allIdleTimeNanos;
            if (!udpConnect.isReading()) {
                nextDelay -= System.nanoTime() -
                        Math.max(udpConnect.getLastReadTime(),
                                udpConnect.getLastWriteTime());
            }
            if (nextDelay <= 0) {
                try {
                    UdpIdleStateEvent event = new UdpIdleStateEvent(IdleState.ALL_IDLE,
                            socketAddress);
                    udpConnects.remove(socketAddress);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                udpConnect.setAllIdleTimeout(eventLoop.schedule(
                        this, allIdleTimeNanos, TimeUnit.NANOSECONDS));
            }
        }
    }
}
