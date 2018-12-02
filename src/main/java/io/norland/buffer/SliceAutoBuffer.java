package io.norland.buffer;

/**
 * 复用byte[]数组的切片
 */
public class SliceAutoBuffer extends AutoBuffer {
    private int adjustReaderIndex;
    private int adjustWriterIndex;

    public SliceAutoBuffer(int readerIndex, int writerIndex, byte[] data) {
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
        adjustReaderIndex = readerIndex;
        adjustWriterIndex = readerIndex;
        this.data = data;
    }

    /**
     * 多层共享byte数组时会出问题
     */
    @Override
    public AutoBuffer slice(int index, int len) {
        return super.slice(index + adjustReaderIndex, len);
    }

    public int readerIndex() {
        return readerIndex - adjustReaderIndex;
    }

    public int writerIndex() {
        return writerIndex - adjustWriterIndex;
    }

}
