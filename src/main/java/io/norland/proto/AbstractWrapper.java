package io.norland.proto;

/**
 * 包装类基类
 */
public abstract class AbstractWrapper{
    /**
     * 请求的协议类型
     */
    public abstract String requestProtocol();

    /**
     * 获取设备序列号
     */
    public abstract String getTerminalSerialNo();

}
