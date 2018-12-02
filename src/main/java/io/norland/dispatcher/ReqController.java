package io.norland.dispatcher;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;

public interface ReqController {
    ActionAndModel handleRequest(AbstractWrapper request,
                                 Object handler,
                                 Channel channel) throws Exception;
}
