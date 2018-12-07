package io.norland.server.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class UdpEchoFrameCodec extends MessageToMessageCodec<DatagramPacket, Object> {

    private InetSocketAddress socketAddress;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        if (socketAddress == null) return;
        if (msg == null) return;
        if (msg instanceof byte[]) {
            buf.readBytes((byte[]) msg);
            DatagramPacket packet = new DatagramPacket(buf, socketAddress);
            out.add(packet);
            log.info("SEND MASSAGE: " + toHexString((byte[]) msg));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          DatagramPacket packet,
                          List<Object> out) throws Exception {
        socketAddress = packet.sender();
        log.info("消息来源 : "
                + socketAddress.getHostString()
                + ":" + socketAddress.getPort());
        ByteBuf in = packet.content();
        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);
        out.add(data);
    }

    private String toHexString(byte[] bts) {
        StringBuilder strBuild = new StringBuilder();

        for (byte bt : bts) {
            strBuild.append(String.format("%02X", bt));
        }
        return strBuild.toString();
    }

}
