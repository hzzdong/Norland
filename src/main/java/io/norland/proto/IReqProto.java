package io.norland.proto;

import io.norland.buffer.AutoBuffer;

import java.io.Serializable;

/**
 * request protocol model
 */
public interface IReqProto extends Serializable {
    /**
     * 读取字节流，解析出数据
     */
    void readFromBuf(AutoBuffer buffer);
}