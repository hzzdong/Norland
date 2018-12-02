package io.norland.proto;


import java.io.Serializable;

/**
 * writable protocol
 */
public interface IProto2 extends WritableProto {

    /**
     * 读取对象，解析出数据
     */
    void readFromObject(Object o);
}