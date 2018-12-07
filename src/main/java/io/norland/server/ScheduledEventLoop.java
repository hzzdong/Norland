package io.norland.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduledEventLoop {
    private final static ReentrantLock lock = new ReentrantLock();
    private int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);

    private ScheduledThreadPoolExecutor scheduledPool =
            new ScheduledThreadPoolExecutor(DEFAULT_EVENT_LOOP_THREADS);

    private static ScheduledEventLoop eventLoop;

    private ScheduledEventLoop() {
    }

    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit) {
        return scheduledPool.schedule(command,
                delay, unit);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit) {
        return scheduledPool.schedule(callable,
                delay, unit);
    }

    public void shutdown() {
        lock.lock();
        try {
            if (!scheduledPool.isShutdown()) {
                scheduledPool.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }

    public static ScheduledEventLoop getInstance() {
        if (eventLoop == null)
            synchronized (ScheduledEventLoop.class) {
                if (eventLoop == null) {
                    eventLoop = new ScheduledEventLoop();
                }
            }
        return eventLoop;
    }
}
