package io.norland.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class ChannelHolder {

    private final static ChannelGroup allChannels
            = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final static ConcurrentHashMap<String, ChannelId> channelIdMap
            = new ConcurrentHashMap<>();

    private final static AttributeKey<String> attributeKey = AttributeKey.valueOf("terminalSerialNo");
    private final static AttributeKey<SharedParam> sharedParamAttributeKey = AttributeKey.valueOf("sharedParam");


    public static void send(String serialNo, Object socketFrame) {
        ChannelId channelId = channelIdMap.get(serialNo);
        if (channelId != null) {
            Channel channel = allChannels.find(channelId);
            if (channel != null) {
                channel.writeAndFlush(socketFrame);
            }
        }
    }

    public static void send(List<String> serialNos, Object socketFrame) {
        List<ChannelId> channelIds = serialNos.stream()
                .map(serialNo -> channelIdMap.get(serialNo))
                .filter(channelId -> channelId != null)
                .collect(Collectors.toList());
        allChannels.writeAndFlush(socketFrame, channel -> channelIds.contains(channel.id()));
    }

    public static void broadcast(Object socketFrame) {
        allChannels.writeAndFlush(socketFrame);
    }

    public static void remove(String serialNo) {
        ChannelId channelId = channelIdMap.get(serialNo);
        if (channelId != null) {
            Channel channel = allChannels.find(channelId);
            if (channel != null) {
                allChannels.remove(channel);
            }
        }
    }

    public static void remove(Channel channel) {
        allChannels.remove(channel);
        String serialNo = channel.attr(attributeKey).get();
        if (serialNo != null) {
            channelIdMap.remove(serialNo);
            return;
        }
        for (Map.Entry<String, ChannelId> entry : channelIdMap.entrySet()) {
            if (entry.getValue().equals(channel.id())) {
                channelIdMap.remove(entry.getKey());
                return;
            }
        }
    }

    public static void add(Channel channel) {
        allChannels.add(channel);
    }

    public static void bindSerialNoWithChannel(String serialNo, Channel channel) {
        channelIdMap.put(serialNo, channel.id());
        channel.attr(attributeKey).setIfAbsent(serialNo);
    }

    public static List<String> getTerminalSerialNos() {
        return new ArrayList<>(channelIdMap.keySet());
    }

    public static String getTerminalSerialNoByChannel(Channel channel) {
        return channel.attr(attributeKey).get();
    }

    public static SharedParam getSharedParamByChannel(Channel channel) {
        return channel.attr(sharedParamAttributeKey).get();
    }

    public static void setSharedParam(Channel channel, SharedParam sharedParam) {
        channel.attr(sharedParamAttributeKey).setIfAbsent(sharedParam);
    }

    public static Channel getChannelBySerialNo(String serialNo) {
        ChannelId channelId = channelIdMap.get(serialNo);
        if (channelId != null) {
            return allChannels.find(channelId);
        }
        return null;
    }

    public static boolean containsKey(String serialNo) {
        return channelIdMap.containsKey(serialNo);
    }
}
