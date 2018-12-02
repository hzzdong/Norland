package io.norland.proto;

import java.io.Serializable;

/**
 * writable protocol
 */
public interface WritableProto extends Serializable {
    /**
     * 数据转成字节流
     */
    byte[] writeToBytes();
}
