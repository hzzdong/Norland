package io.norland.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.norland.autoconfigure.ProtoProperties;
import io.norland.dispatcher.Dispatcher;
import io.norland.server.tcp.TcpDispatchFrameHandler;
import io.norland.server.udp.UdpDispatchFrameHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于NETTY框架服务器
 */
@Slf4j
public class NettyServer {

    private ProtoProperties properties;
    private Dispatcher dispatcher;
    private ProtoChannelInitializer protoChannelInitializer;

    private String serverType;//udp tcp
    private int listenPort;
    private String leakDetectorLevel;

    private ChannelFuture f;
    private List<ChannelFuture> futureList = new ArrayList<>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(ProtoProperties properties,
                       ProtoChannelInitializer protoChannelInitializer,
                       Dispatcher dispatcher) {
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.protoChannelInitializer = protoChannelInitializer;
    }

    private void initParams() {
        serverType = properties.getServerType() == null ?
                "tcp" : properties.getServerType();
        listenPort = properties.getListenPort() == null ?
                8633 : properties.getListenPort();
        leakDetectorLevel = properties.getLeakDetectorLevel() == null ?
                "DEFAULT" : properties.getLeakDetectorLevel();
    }

    public void startTcp() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)
                            throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        appendUserHandle(pipeline);
                        pipeline.addLast(new TcpDispatchFrameHandler(dispatcher));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        setNettyLeakDetectorLevel();
        // Bind and startTcp to accept incoming connections.
        f = b.bind(listenPort).sync()
                .addListener(future ->
                        log.info("NettyService " +
                                "startTcp success (TCP"
                                + listenPort + ")"));
    }

    public void startUdp() throws Exception {
        int epollNum = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
        InetSocketAddress address = new InetSocketAddress(listenPort);
        EventLoopGroup group = Epoll.isAvailable() ?
                new EpollEventLoopGroup(epollNum) : new NioEventLoopGroup();
        Class<? extends Channel> channel = Epoll.isAvailable() ?
                EpollDatagramChannel.class : NioDatagramChannel.class;
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(channel)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch)
                            throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        appendUserHandle(pipeline);
                        pipeline.addLast(new UdpDispatchFrameHandler(dispatcher));
                    }
                });
        if (Epoll.isAvailable()) {
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    // linux系统下使用SO_REUSEPORT特性（提高性能），使得多个线程绑定同一个端口
                    .option(EpollChannelOption.SO_REUSEPORT, true);
            log.info("using epoll reuseport and epollNum:" + epollNum);
            for (int i = 0; i < epollNum; ++i) {
                ChannelFuture future = bootstrap.bind(address).await();
                if (!future.isSuccess())
                    throw new Exception(String.format("Fail to bind on [host = %s , port = %d].",
                            address.getHostString(),
                            address.getPort()), future.cause());
                futureList.add(future);
            }
        } else {
            // 绑定端口，开始接收进来的连接
            f = bootstrap.bind(listenPort).sync();
        }
        log.info("NettyService " +
                "startUdp success (UDP"
                + listenPort + ")");
    }

    private void appendUserHandle(ChannelPipeline pipeline)
            throws Exception {
        if (protoChannelInitializer != null) {
            protoChannelInitializer.initChannel(pipeline);
        }
    }

    private void setNettyLeakDetectorLevel() {
        switch (leakDetectorLevel) {
            case "DISABLED":
                ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
                break;
            case "SIMPLE":
                ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.SIMPLE);
                break;
            case "ADVANCED":
                ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
                break;
            case "PARANOID":
                ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        }
    }

    @PostConstruct
    public void postStartNetty() {
        try {
            if (properties.isDispatcherEnabled()) {
                initParams();
                if ("udp".equalsIgnoreCase(serverType)) {
                    startUdp();
                } else if ("tcp".equalsIgnoreCase(serverType)) {
                    startTcp();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("NettyService start " + serverType + " fail");
        }
    }

    @PreDestroy
    public void PreDestroy() {
        if (f != null) {
            f.channel().close().awaitUninterruptibly();
        }
        for (ChannelFuture f : futureList) {
            f.channel().close().awaitUninterruptibly();
        }
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
        log.info("NettyService stop success (" + listenPort + ")");
    }
}
