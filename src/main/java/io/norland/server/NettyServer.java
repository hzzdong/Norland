package io.norland.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import io.norland.autoconfigure.ProtoProperties;
import io.norland.dispatcher.Dispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 基于NETTY框架服务器
 */
@Slf4j
public class NettyServer {
    private static Logger logger = Logger.getLogger(NettyServer.class);

    private ProtoProperties properties;
    private Dispatcher dispatcher;
    private ProtoChannelInitializer protoChannelInitializer;

    private String serverType;//udp tcp
    private int listenPort;
    private long readerIdleTime;
    private long writerIdleTime;
    private long allIdleTime;
    private String leakDetectorLevel;

    private ChannelFuture f;
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
        readerIdleTime = properties.getReaderIdleTime() == null ?
                30 : properties.getReaderIdleTime();
        writerIdleTime = properties.getWriterIdleTime() == null ?
                0 : properties.getWriterIdleTime();
        allIdleTime = properties.getAllIdleTime() == null ?
                0 : properties.getAllIdleTime();
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
                        appendHandlesToPipeline(ch.pipeline());
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
        Bootstrap b = new Bootstrap();
        workerGroup = new NioEventLoopGroup();
        b.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel(NioDatagramChannel ch)
                            throws Exception {
                        appendHandlesToPipeline(ch.pipeline());
                    }
                }).option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024);

        // 绑定端口，开始接收进来的连接
        f = b.bind(listenPort).sync()
                .addListener(future ->
                        log.info("NettyService " +
                                "startUdp success (UDP"
                                + listenPort + ")"));
    }

    private void appendHandlesToPipeline(ChannelPipeline pipeline)
            throws Exception {
        appendHeartBeat(pipeline);
        appendUserHandle(pipeline);
        appendDispatcher(pipeline);
    }

    private void appendDispatcher(ChannelPipeline pipeline) {
        pipeline.addLast(new ProtoDispatchFrameHandler(dispatcher));
    }

    private void appendUserHandle(ChannelPipeline pipeline)
            throws Exception {
        if (protoChannelInitializer != null) {
            protoChannelInitializer.initChannel(pipeline);
        }
    }

    private void appendHeartBeat(ChannelPipeline pipeline) {
        pipeline.addLast(new IdleStateHandler(readerIdleTime,
                writerIdleTime,
                allIdleTime,
                TimeUnit.MINUTES));
        pipeline.addLast(new HeartBeatServerHandler());
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
            log.error("NettyService startTcp fail");
        }
    }

    @PreDestroy
    public void PreDestroy() {
        if (f != null) {
            f.channel().close().awaitUninterruptibly();
        }
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
        log.info("NettyService stop success (" + listenPort + ")");
    }
}
