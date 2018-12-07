package io.norland.server.udp;

import lombok.Data;

import java.util.concurrent.ScheduledFuture;

@Data
public class UdpConnect {

    private volatile ScheduledFuture<?> readerIdleTimeout;
    private volatile long lastReadTime;
    private boolean firstReaderIdleEvent = true;

    private volatile ScheduledFuture<?> writerIdleTimeout;
    private volatile long lastWriteTime;
    private boolean firstWriterIdleEvent = true;

    private volatile ScheduledFuture<?> allIdleTimeout;
    private boolean firstAllIdleEvent = true;

    private volatile boolean reading;

    public UdpConnect() {
    }

    public void destroy() {
        if (readerIdleTimeout != null) {
            readerIdleTimeout.cancel(false);
            readerIdleTimeout = null;
        }
        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
        if (allIdleTimeout != null) {
            allIdleTimeout.cancel(false);
            allIdleTimeout = null;
        }
    }
}
