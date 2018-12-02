package io.norland.proto;


import io.norland.buffer.AutoBuffer;

import java.io.Serializable;

/**
 * writable protocol
 */
public interface IProto extends WritableProto {

    /**
     * 读取字节流，解析出数据
     */
    void readFromBuf(AutoBuffer buffer);
}