package io.norland.server.udp;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class InetAddressHolder {

    private static final AtomicInteger idx = new AtomicInteger();

    private static List<Channel> channelList = new ArrayList<>();

    private final static ConcurrentHashMap<Integer, InetSocketAddress> addressHashMap
            = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<String, Integer> serialNoAddressMap
            = new ConcurrentHashMap<>();

    public static void send(String serialNo, Object socketFrame) {
        Integer hashCode = serialNoAddressMap.get(serialNo);
        if (hashCode != null) {
            UdpMessage udpMessage = new UdpMessage();
            udpMessage.setSerialNo(serialNo);
            udpMessage.setAddress(addressHashMap.get(hashCode));
            udpMessage.setValue(socketFrame);
            innerSender(udpMessage);
        }
    }

    public static void send(List<String> serialNos, Object socketFrame) {
        for (String serialNo : serialNos) {
            send(serialNo, socketFrame);
        }
    }

    private static void innerSender(UdpMessage udpMessage) {
        if (channelList.size() == 0)
            return;
        int index = next();
        channelList.get(index).writeAndFlush(udpMessage);
    }

    private static int next() {
        int size = channelList.size();
        if (size <= 1)
            return 0;
        return idx.getAndIncrement() % channelList.size();
    }

    public static void removeUdpChannel(Channel channel) {
        channelList.remove(channel);
        if (channelList.size() == 0) {
            addressHashMap.clear();
            serialNoAddressMap.clear();
        }
    }

    public static void addChannel(Channel channel) {
        channelList.add(channel);
    }

    public static void bindSerialNoWithInetSocketAddress(String serialNo,
                                                         InetSocketAddress inetSocketAddress) {
        serialNoAddressMap.put(serialNo, Objects.hash(inetSocketAddress));
    }

    public static String getTerminalSerialNoByInetSocketAddress(InetSocketAddress address) {
        Integer hashCode = Objects.hash(address);
        for (Map.Entry<String, Integer> entry : serialNoAddressMap.entrySet()) {
            if (entry.getValue().equals(hashCode)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<String> getTerminalSerialNos() {
        return new ArrayList<>(serialNoAddressMap.keySet());
    }

    public static boolean containsKey(String serialNo) {
        return serialNoAddressMap.containsKey(serialNo);
    }
}
