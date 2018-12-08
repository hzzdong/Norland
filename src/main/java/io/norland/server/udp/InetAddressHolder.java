package io.norland.server.udp;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class InetAddressHolder {

    private static final AtomicInteger idx = new AtomicInteger();

    private static final List<Channel> channelGroup = new ArrayList<>();

    private static final ConcurrentHashMap<String, InetSocketAddress> serialNoAddressMap
            = new ConcurrentHashMap<>();

    public static void send(String serialNo, Object socketFrame) {
        InetSocketAddress address = serialNoAddressMap.get(serialNo);
        UdpMessage udpMessage = new UdpMessage();
        udpMessage.setSerialNo(serialNo);
        udpMessage.setAddress(address);
        udpMessage.setValue(socketFrame);
        chooseChannelAndSend(udpMessage);
    }

    public static void send(List<String> serialNos, Object socketFrame) {
        for (String serialNo : serialNos) {
            send(serialNo, socketFrame);
        }
    }

    private static void chooseChannelAndSend(UdpMessage udpMessage) {
        if (channelGroup.size() == 0)
            return;
        int index = next();
        channelGroup.get(index).writeAndFlush(udpMessage);
    }

    private static int next() {
        int size = channelGroup.size();
        if (size <= 1)
            return 0;
        return idx.getAndIncrement() % channelGroup.size();
    }

    public static void remove(Channel channel) {
        channelGroup.remove(channel);
    }

    public static void add(Channel channel) {
        channelGroup.add(channel);
    }

    public static void bind(String serialNo, InetSocketAddress address) {
        serialNoAddressMap.put(serialNo, address);
    }

    public static String getSerialNo(InetSocketAddress address) {
        for (Map.Entry<String, InetSocketAddress> entry
                : serialNoAddressMap.entrySet()) {
            if (entry.getValue().equals(address)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<String> getSerialNoList() {
        return new ArrayList<>(serialNoAddressMap.keySet());
    }

    public static boolean containsKey(String serialNo) {
        return serialNoAddressMap.containsKey(serialNo);
    }

    public static void clearSerialNos() {
        serialNoAddressMap.clear();
    }

    public static void clearChannelGroup() {
        channelGroup.clear();
    }
}
