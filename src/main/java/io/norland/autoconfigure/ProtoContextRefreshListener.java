package io.norland.autoconfigure;

import io.norland.dispatcher.Dispatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ProtoContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

    private Dispatcher dispatcher;

    public ProtoContextRefreshListener(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * 延时初始化Dispatcher
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        dispatcher.onRefresh(context);
    }
}
