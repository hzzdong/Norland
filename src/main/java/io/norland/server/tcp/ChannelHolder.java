package io.norland.server.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelHolder {

    private final static ChannelGroup channelGroup
            = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final static ConcurrentHashMap<String, ChannelId> serialNoChannelIdMap
            = new ConcurrentHashMap<>();

    private final static AttributeKey<String> serialNoAttributeKey =
            AttributeKey.valueOf("serialNo");

    public static void send(String serialNo, Object socketFrame) {
        ChannelId channelId = serialNoChannelIdMap.get(serialNo);
        if (channelId != null) {
            Channel channel = channelGroup.find(channelId);
            if (channel != null) {
                channel.writeAndFlush(socketFrame);
            }
        }
    }

    public static void send(List<String> serialNos, Object socketFrame) {
        List<ChannelId> channelIdList = new ArrayList<>();
        for (String serialNo : serialNos) {
            ChannelId channelId = serialNoChannelIdMap.get(serialNo);
            if (channelId != null) {
                channelIdList.add(channelId);
            }
        }
        channelGroup.writeAndFlush(socketFrame, new ChannelMatcher() {
            @Override
            public boolean matches(Channel channel) {
                ChannelId channelId = channel.id();
                return channelIdList.contains(channelId);
            }
        });
    }

    public static void broadcast(Object socketFrame) {
        channelGroup.writeAndFlush(socketFrame);
    }

    public static void remove(String serialNo) {
        ChannelId channelId = serialNoChannelIdMap.get(serialNo);
        if (channelId != null) {
            Channel channel = channelGroup.find(channelId);
            if (channel != null) {
                channelGroup.remove(channel);
            }
        }
    }

    public static void remove(Channel channel) {
        channelGroup.remove(channel);
        String serialNo = channel.attr(serialNoAttributeKey).get();
        if (serialNo != null) {
            serialNoChannelIdMap.remove(serialNo);
            return;
        }
        for (Map.Entry<String, ChannelId> entry : serialNoChannelIdMap.entrySet()) {
            if (entry.getValue().equals(channel.id())) {
                serialNoChannelIdMap.remove(entry.getKey());
                return;
            }
        }
    }

    public static void add(Channel channel) {
        channelGroup.add(channel);
    }

    public static void bind(String serialNo, Channel channel) {
        serialNoChannelIdMap.put(serialNo, channel.id());
        channel.attr(serialNoAttributeKey).setIfAbsent(serialNo);
    }

    public static List<String> getSerialNoList() {
        return new ArrayList<>(serialNoChannelIdMap.keySet());
    }

    public static String getSerialNo(Channel channel) {
        return channel.attr(serialNoAttributeKey).get();
    }

    public static Channel getChannel(String serialNo) {
        ChannelId channelId = serialNoChannelIdMap.get(serialNo);
        if (channelId != null) {
            return channelGroup.find(channelId);
        }
        return null;
    }

    public static boolean containsKey(String serialNo) {
        return serialNoChannelIdMap.containsKey(serialNo);
    }

    public static void clearSerialNos() {
        serialNoChannelIdMap.clear();
    }

    public static void clearChannelGroup() {
        channelGroup.close();
    }
}
