package io.norland.buffer;

import java.io.UnsupportedEncodingException;

/**
 * 读写byte[]的工具类
 */
public class AutoBuffer {

    protected int readerIndex;
    protected int writerIndex;
    protected int markedReaderIndex;
    protected int markedWriterIndex;
    protected byte[] data;
    private int maxCapacity = 1024 * 6;

    public AutoBuffer() {
        this.data = new byte[1024];
        this.readerIndex = 0;
        this.writerIndex = 0;
    }

    public AutoBuffer(byte[] data) {
        this.data = data;
        this.readerIndex = 0;
        this.writerIndex = data.length;
    }

    public byte readByte() {
        checkReadableBytes0(1);
        byte b = data[readerIndex];
        readerIndex += 1;
        return b;
    }

    public byte[] readBytes(int len) {
        checkReadableBytes0(len);
        byte[] bytes = new byte[len];
        System.arraycopy(data, readerIndex, bytes, 0, len);
        readerIndex += len;
        return bytes;
    }

    public byte[] readBytes() {
        checkReadableBytes0(readableBytes());
        byte[] bytes = array();
        readerIndex += readableBytes();
        return bytes;
    }

    public byte[] array() {
        if (readerIndex == 0 && writerIndex == data.length) {
            return data;
        }
        byte[] bytes = new byte[writerIndex - readerIndex];
        System.arraycopy(data, readerIndex, bytes, 0, bytes.length);
        return bytes;
    }

    public boolean isReadable() {
        return writerIndex - readerIndex > 0;
    }

    public boolean isWriteable() {
        return maxCapacity - writerIndex > 0;
    }

    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    public long readLong() {
        checkReadableBytes0(8);
        Long v = ((long) data[readerIndex] & 0xFF) << 56 |
                ((long) data[readerIndex + 1] & 0xFF) << 48 |
                ((long) data[readerIndex + 2] & 0xFF) << 40 |
                ((long) data[readerIndex + 3] & 0xFF) << 32 |
                ((long) data[readerIndex + 4] & 0xFF) << 24 |
                ((long) data[readerIndex + 5] & 0xFF) << 16 |
                ((long) data[readerIndex + 6] & 0xFF) << 8 |
                (long) data[readerIndex + 7] & 0xFF;
        this.readerIndex += 8;
        return v;
    }

    public int readInt() {
        checkReadableBytes0(4);
        int v = (data[readerIndex] & 0xFF) << 24 |
                (data[readerIndex + 1] & 0xFF) << 16 |
                (data[readerIndex + 2] & 0xFF) << 8 |
                data[readerIndex + 3] & 0xFF;
        this.readerIndex += 4;
        return v;
    }

    public short readShort() {
        checkReadableBytes0(2);
        short v = (short) ((data[readerIndex] & 0xFF) << 8 |
                (data[readerIndex + 1] & 0xFF));
        this.readerIndex += 2;
        return v;
    }

    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public String readString() {
        return new String(readBytes());
    }

    public String readString(int len) {
        return new String(readBytes(len));
    }

    public String readGbkString() {
        try {
            return new String(readBytes(), "gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String readGbkString(int len) {
        try {
            return new String(readBytes(len), "gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String readBcd() {
        return readBcdString(1);
    }

    public String readBcdString() {
        return readBcdString(readableBytes());
    }

    /**
     * BCD一个byte表示两个十进制数(4个bit一个)
     * 即byte的高4bit表示一个数低4bit表示一个数
     * BCD只表示1-9
     */
    public String readBcdString(int len) {
        byte[] bytes = readBytes(len);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int hi4 = (b & 0xF0) >> 4;
            int lo4 = b & 0x0F;
            if (lo4 < 0x0A) {//
                // 高四位
                sb.append(hi4);
                // 低四位
                sb.append(lo4);
            } else {
                // 低四位
                sb.append(lo4);
            }

        }
        return sb.toString();
    }

    private String byteToHexString(byte b) {
        int bt = b & 0xFF;
        return String.format("%02X", bt);
    }

    private byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) return null;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int bt = Integer.valueOf(hex.substring((i + 1) * 2 - 1 - 1, (i + 1) * 2 - 1 + 1), 16);
            bytes[i] = (byte) (bt & 0xFF);
        }
        return bytes;
    }

    public String readHexString() {
        return readHexString(readableBytes());
    }

    public String readHexString(int len) {
        byte[] bytes = readBytes(len);
        StringBuilder strBuild = new StringBuilder();
        for (byte bt : bytes) {
            strBuild.append(byteToHexString(bt));
        }
        return strBuild.toString();
    }

    public char readChar() {
        return (char) readShort();
    }

    public String readAsciiString(int length) {
        StringBuilder result = new StringBuilder();
        byte[] bytes = readBytes(length);
        for (byte b : bytes) {
            result.append((char) b);
        }
        return result.toString();
    }

    public void writeAsciiString(String ascii) {
        writeAsciiString(ascii, ascii.length());
    }

    public void writeAsciiString(String ascii, int length) {
        ensureWritable0(length);
        String str = ascii.substring(0, length);
        byte[] bytes = new byte[length];
        for (int i = 0; i < str.length(); i++) {
            bytes[i] = (byte) str.charAt(i);
        }
        writeBytes(bytes);
    }

    public void skipBytes(int len) {
        this.readerIndex += len;
    }

    public void markReaderIndex() {
        this.markedReaderIndex = this.readerIndex;
    }

    public void markedWriterIndex() {
        this.markedWriterIndex = this.writerIndex;
    }

    public void resetReaderIndex() {
        this.readerIndex = this.markedReaderIndex;
    }

    public void resetWriterIndex() {
        this.writerIndex = this.markedWriterIndex;
    }


    private void checkReadableBytes0(int minimumReadableBytes) {
        if (writerIndex <= readerIndex + minimumReadableBytes - 1) {
            throw new IndexOutOfBoundsException(String.format(
                    "writerIndex(%d) + readerIndex(%d) + length(%d): %s", writerIndex,
                    readerIndex, minimumReadableBytes, this));
        }
    }

    @Override
    public String toString() {
        return new String(data);
    }

    public int readerIndex() {
        return readerIndex;
    }

    public int writerIndex() {
        return writerIndex;
    }

    public AutoBuffer slice(int index, int len) {
        AutoBuffer byteReader = new SliceAutoBuffer(index, index + len, data);
        return byteReader;
    }

    public void writeByte(byte a) {
        ensureWritable0(1);
        data[writerIndex] = a;
        writerIndex += 1;
    }

    public void writeShort(short a) {
        ensureWritable0(2);
        data[writerIndex] = (byte) ((a >> 8) & 0xFF);
        data[writerIndex + 1] = (byte) (a & 0xFF);
        writerIndex += 2;
    }

    public void writeBytes(byte[] a) {
        ensureWritable0(a.length);
        System.arraycopy(a, 0, data, writerIndex, a.length);
        writerIndex += a.length;
    }

    public void writeInt(int a) {
        ensureWritable0(4);
        data[writerIndex + 0] = (byte) ((a >> 24) & 0xFF);
        data[writerIndex + 1] = (byte) ((a >> 16) & 0xFF);
        data[writerIndex + 2] = (byte) ((a >> 8) & 0xFF);
        data[writerIndex + 3] = (byte) (a & 0xFF);
        writerIndex += 4;
    }

    public void writeLong(long a) {
        ensureWritable0(8);
        data[writerIndex + 0] = (byte) ((a >> 56) & 0xFF);
        data[writerIndex + 1] = (byte) ((a >> 48) & 0xFF);
        data[writerIndex + 2] = (byte) ((a >> 40) & 0xFF);
        data[writerIndex + 3] = (byte) ((a >> 32) & 0xFF);
        data[writerIndex + 4] = (byte) ((a >> 24) & 0xFF);
        data[writerIndex + 5] = (byte) ((a >> 16) & 0xFF);
        data[writerIndex + 6] = (byte) ((a >> 8) & 0xFF);
        data[writerIndex + 7] = (byte) (a & 0xFF);
        writerIndex += 8;
    }

    public void writeShort(int a) {
        writeShort((short) a);
    }

    public void writeInt(long a) {
        writeInt((int) a);
    }

    public void writeGbkString(String str) {
        try {
            byte[] b = str.getBytes("gbk");
            writeBytes(b);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void writeGbkString(String str, int len) {
        byte[] result = new byte[len];
        try {
            byte[] b = str.getBytes("gbk");
            System.arraycopy(b, 0, result, 0, b.length);
            for (int m = b.length; m < len; m++) {
                result[m] = 0;
            }
            writeBytes(result);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void writeHexString(String hex) {
        byte[] bytes = hexStringToBytes(hex);
        writeBytes(bytes);
    }

    public void writeHexString(String hex, int len) {
        byte[] bytes = hexStringToBytes(hex);
        if (bytes != null) {
            byte[] bytesFromWrite = new byte[len];
            System.arraycopy(bytes, 0, bytesFromWrite, 0, len);
            writeBytes(bytesFromWrite);
        }
    }

    public void writeBcdString(String bcd) {
        writeBcdString(bcd, bcd.length() / 2);
    }

    public void writeBcdString(String bcd, int len) {
        byte[] rawBytes = bcdStringToBytes(bcd);
        if (rawBytes != null) {
            byte[] cookedBytes;
            if (rawBytes.length == len)
                cookedBytes = rawBytes;
            else {
                cookedBytes = new byte[len];
                System.arraycopy(rawBytes, 0, cookedBytes, 0, len);
            }
            writeBytes(cookedBytes);
        }
    }

    private byte[] bcdStringToBytes(String bcd) {
        if (bcd.length() % 2 != 0) return null;
        byte[] bytes = new byte[bcd.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = (i + 1) * 2 - 1 - 1;
            int hi4 = (int) bcd.charAt(index) - '0';
            int lo4 = bcd.charAt(index + 1) - '0';
            bytes[i] = (byte) ((byte) ((hi4 & 0xFF) << 4) |
                    ((lo4 & 0xFF)));
        }
        return bytes;
    }

    public void writeString(String str) {
        byte[] b = str.getBytes();
        writeBytes(b);
    }

    public void writeString(String str, int len) {
        byte[] result = new byte[len];
        byte[] b = str.getBytes();
        System.arraycopy(b, 0, result, 0, b.length);
        for (int m = b.length; m < len; m++) {
            result[m] = 0;
        }
        writeBytes(result);
    }

    public int writableBytes() {
        return data.length - writerIndex;
    }

    private void ensureWritable0(int minWritableBytes) {
        if (minWritableBytes <= writableBytes()) {
            return;
        }
        if (minWritableBytes > maxCapacity - writerIndex) {
            throw new IndexOutOfBoundsException(String.format(
                    "writerIndex(%d) + minWritableBytes(%d) exceeds maxCapacity(%d): %s",
                    writerIndex, minWritableBytes, maxCapacity, this));
        }
        int tmpLen = data.length + 1024 + minWritableBytes;
        int len = tmpLen > maxCapacity ? maxCapacity : tmpLen;
        byte[] bytes = new byte[len];
        System.arraycopy(data, 0, bytes, 0, data.length);
        data = bytes;
    }

    public void clear() {
        this.data = new byte[0];
        this.readerIndex = 0;
        this.writerIndex = 0;
        this.markedReaderIndex = 0;
        this.markedWriterIndex = 0;
    }

    public void writeByte(int b) {
        writeByte((byte) b);
    }

}
