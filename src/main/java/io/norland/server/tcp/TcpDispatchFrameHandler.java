package io.norland.server.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.norland.proto.AbstractWrapper;
import io.norland.dispatcher.Dispatcher;

public class TcpDispatchFrameHandler extends SimpleChannelInboundHandler<AbstractWrapper> {

    private Dispatcher dispatcher;

    public TcpDispatchFrameHandler(Dispatcher dispatcher) {
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
        Object value = dispatcher.dispatch(req, ctx.channel());
        if (value != null) {
            ctx.channel().writeAndFlush(value);
        }
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