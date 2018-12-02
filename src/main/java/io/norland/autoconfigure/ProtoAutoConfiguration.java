package io.norland.autoconfigure;

import io.netty.channel.ChannelPipeline;
import io.norland.dispatcher.Dispatcher;
import io.norland.server.example.DiscardFrameCodec;
import io.norland.server.example.DiscardFrameHandler;
import io.norland.server.NettyServer;
import io.norland.server.ProtoChannelInitializer;
import io.norland.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ProtoProperties.class})
public class ProtoAutoConfiguration {

    /**
     * 请求分发处理器
     */
    @Bean
    public Dispatcher dispatcher() {
        return new Dispatcher();
    }

    /**
     * 在spring容器初始化完成后
     * 初始化Dispatcher的Mapping以及Adapter
     */
    @Bean
    @ConditionalOnBean(Dispatcher.class)
    public ProtoContextRefreshListener applicationStartListener(Dispatcher dispatcher) {
        return new ProtoContextRefreshListener(dispatcher);
    }

    /**
     * Netty ProtoChannelInitializer缺省时该类加入spring容器
     */
    @Bean
    @ConditionalOnBean(Dispatcher.class)
    @ConditionalOnMissingBean(ProtoChannelInitializer.class)
    public ProtoChannelInitializer protoChannelInitializer() {
        return new ProtoChannelInitializer() {
            @Override
            public void initChannel(ChannelPipeline pipeline) throws Exception {
                pipeline.addLast(new DiscardFrameCodec())
                        .addLast(new DiscardFrameHandler());
            }
        };
    }

    /**
     * 启动Netty
     */
    @Bean
    @ConditionalOnBean({Dispatcher.class, ProtoChannelInitializer.class})
    public NettyServer nettyServer(ProtoProperties properties,
                                   ProtoChannelInitializer protoChannelInitializer,
                                   Dispatcher dispatcher) {
        return new NettyServer(properties, protoChannelInitializer, dispatcher);
    }

    /**
     * 启动长时处理队列
     */
    @Bean
    public QueueService queueService(ProtoProperties properties) {
        return new QueueService(properties);
    }

}
