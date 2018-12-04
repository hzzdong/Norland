package io.norland.proto;

/**
 * command protocol model
 */
public interface ICmdProto extends WritableProto {
    /**
     * 一般通过REST的方式或者从Database中读取的数据
     * 都是以字符串的形式存在
     */
    void readFromFormatString(String formatString);
}