package io.norland.dispatcher;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;

public class ReqHandlerAdapter implements HandlerAdapter {
    public boolean supports(Object handler) {
        return handler instanceof ReqController;
    }

    @Override
    public Object handle(Object value, Object handler, Object... moreParams) throws Exception {
        return ((ReqController) handler)
                .handleRequest((AbstractWrapper) value,
                        handler,
                        (Channel) moreParams[0]);
    }
}