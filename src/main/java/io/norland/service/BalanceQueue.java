package io.norland.service;



import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BalanceQueue<E> {
    private ConcurrentLinkedQueue<E>[] queues;
    private Chooser pushChooser;
    private Chooser pollChooser;
    private int queueNum = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);

    @SuppressWarnings("unchecked")
    public BalanceQueue() {
        queues = new ConcurrentLinkedQueue[queueNum];
        for (int i = 0; i < queueNum; i++) {
            queues[i] = new ConcurrentLinkedQueue<E>();
        }
        pushChooser = new Chooser(queueNum);
        pollChooser = new Chooser(queueNum);
    }

    public void add(E e) {
        queues[pushChooser.next()].add(e);
    }

    public E poll() {
        int currIndex = 0;
        do {
            E e = queues[pollChooser.next()].poll();
            if (e != null) {
                return e;
            }
            currIndex = currIndex + 1;
        } while (currIndex < queueNum - 1);
        return null;
    }

    private final class Chooser {
        private final AtomicInteger idx = new AtomicInteger();
        private final int length;

        Chooser(int length) {
            this.length = length;
        }

        public int next() {
            return idx.getAndIncrement() % length;
        }
    }
}
