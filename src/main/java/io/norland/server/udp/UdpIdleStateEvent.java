package io.norland.server.udp;

import io.netty.handler.timeout.IdleState;

import java.net.InetSocketAddress;

public class UdpIdleStateEvent {
    private final IdleState state;

    private final InetSocketAddress address;

    public UdpIdleStateEvent(IdleState state, InetSocketAddress address) {
        this.state = state;
        this.address = address;
    }

    public IdleState state() {
        return state;
    }

    public InetSocketAddress address() {
        return address;
    }

}
