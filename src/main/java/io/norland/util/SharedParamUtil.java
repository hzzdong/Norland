package io.norland.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.norland.server.SharedParam;

public class SharedParamUtil {
    private final static AttributeKey<SharedParam> sharedParamAttributeKey
            = AttributeKey.valueOf("sharedParam");

    public static SharedParam getSharedParamByChannel(Channel channel) {
        return channel.attr(sharedParamAttributeKey).get();
    }

    public static void setSharedParam(Channel channel, SharedParam sharedParam) {
        channel.attr(sharedParamAttributeKey).setIfAbsent(sharedParam);
    }
}
