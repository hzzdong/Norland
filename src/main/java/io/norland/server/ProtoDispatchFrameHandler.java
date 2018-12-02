package io.norland.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.norland.proto.AbstractWrapper;
import io.norland.dispatcher.Dispatcher;
import io.norland.server.ChannelHolder;

@ChannelHandler.Sharable
public class ProtoDispatchFrameHandler extends SimpleChannelInboundHandler<AbstractWrapper> {

    private Dispatcher dispatcher;

    public ProtoDispatchFrameHandler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractWrapper req) throws Exception {
        String serialNo = req.getTerminalSerialNo();
        if (serialNo != null && !serialNo.equals("")) {
            if (!ChannelHolder.containsKey(serialNo)) {
                ChannelHolder.bindSerialNoWithChannel(serialNo, ctx.channel());
            }
        }
        dispatcher.dispatch(req, ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ChannelHolder.add(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ChannelHolder.remove(ctx.channel());
    }
}