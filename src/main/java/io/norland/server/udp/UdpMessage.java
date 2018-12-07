package io.norland.server.udp;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class UdpMessage {
    private String serialNo;
    private InetSocketAddress address;
    private Object value;
}
