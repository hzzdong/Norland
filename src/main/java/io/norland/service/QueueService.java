package io.norland.service;

import io.norland.autoconfigure.ProtoProperties;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 处理耗时较长的操作比如数据库存储等
 */
@Slf4j
public class QueueService {

    private ProtoProperties properties;

    private BalanceQueue<Function> dataQueue = new BalanceQueue<>();
    private ThreadPoolExecutor consumingLongTimeExecutor;
    private boolean stopFlag = false;

    private int coreThreadNum;
    private int maxThreadNum;
    private int currentThreadNum;

    public QueueService(ProtoProperties properties) {
        this.properties = properties;
    }

    public void push(Function function) {
        dataQueue.add(function);
    }

    private void consumingLongTimeExecutorStart() {
        consumingLongTimeExecutor = new ThreadPoolExecutor(coreThreadNum, maxThreadNum, 30L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ThreadGroup threadGroup = new ThreadGroup("consumingLongTime");
        for (int m = 0; m < currentThreadNum; m++) {
            Thread thread = new Thread(threadGroup, () -> pollAndApply());
            thread.setName("LONG TIME EXECUTOR NO:" + m + 1);
            consumingLongTimeExecutor.submit(thread);
        }
        consumingLongTimeExecutor.shutdown();
    }

    private void pollAndApply() {
        while (!stopFlag) {
            try {
                Function function = dataQueue.poll();
                if (function != null) {
                    function.apply();
                    Thread.sleep(50L);
                } else {
                    Thread.sleep(1000 * 6);
                }
            } catch (Exception ex) {
                log.error(Thread.currentThread().getName());
                log.error(ex.getMessage(), ex);
            }
        }
    }

    public ProtoProperties getProperties() {
        return properties;
    }

    public void setProperties(ProtoProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void postStartExecutor() {
        log.info("QueueService start");
        if (properties.isLongTimeExecutorEnabled()) {
            initParams();
            consumingLongTimeExecutorStart();
        }
    }

    private void initParams() {
        coreThreadNum = properties.getCoreThreadNum() == null ?
                3 : properties.getCoreThreadNum();
        maxThreadNum = properties.getMaxThreadNum() == null ?
                6 : properties.getMaxThreadNum();
        currentThreadNum = properties.getCurrentThreadNum() == null ?
                5 : properties.getCurrentThreadNum();
        int availableProcessors = Math.max(1,
                Runtime.getRuntime().availableProcessors() * 2 - 1);
        //如果配置的线程数目大于系统能提供的线程数则线程数为默认值
        if (currentThreadNum > availableProcessors) {
            currentThreadNum = availableProcessors;
        }
    }

    @PreDestroy
    public void PreDestroy() throws Exception {
        stopFlag = true;
        while (!consumingLongTimeExecutor.isShutdown()) {
            Thread.sleep(60L);
        }
        log.info("QueueService Stop");
    }
}
