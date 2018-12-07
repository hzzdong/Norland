package io.norland.proto;

import java.net.InetSocketAddress;

/**
 * 包装类基类
 */
public abstract class AbstractUdpWrapper extends AbstractWrapper {
    /**
     * 请求InetSocket地址
     */
    public abstract InetSocketAddress requestInetSocketAddress();
}
