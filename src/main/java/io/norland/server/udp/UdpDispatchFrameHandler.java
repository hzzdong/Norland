package io.norland.server.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.norland.dispatcher.Dispatcher;
import io.norland.proto.AbstractUdpWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UdpDispatchFrameHandler extends SimpleChannelInboundHandler<AbstractUdpWrapper> {

    private Dispatcher dispatcher;

    public UdpDispatchFrameHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractUdpWrapper req) throws Exception {
        String serialNo = req.getTerminalSerialNo();
        if (serialNo != null && !serialNo.equals("")) {
            if (!InetAddressHolder.containsKey(serialNo)) {
                InetAddressHolder.bind(serialNo, req.requestInetSocketAddress());
            }
        }
        Object value = dispatcher.dispatch(req, ctx.channel());
        if (value != null) {
            UdpMessage udpMessage = new UdpMessage();
            udpMessage.setSerialNo(serialNo);
            udpMessage.setAddress(req.requestInetSocketAddress());
            udpMessage.setValue(value);
            ctx.channel().writeAndFlush(udpMessage);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        InetAddressHolder.add(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        InetAddressHolder.remove(ctx.channel());
    }
}