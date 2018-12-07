package io.norland.dispatcher;

import io.netty.channel.Channel;
import io.norland.annotations.ReqMapping;
import io.netty.channel.ChannelHandlerContext;
import io.norland.dispatcher.interceptor.HandlerInterceptor;
import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;
import io.norland.response.Actions;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dispatcher {
    private List<HandlerMapping> reqHandlerMappings;
    private List<HandlerAdapter> reqHandlerAdapters;
    private List<HandlerInterceptor> handlerInterceptors;
    private String[] reqHandlerNames = new String[]{};

    public Dispatcher() {
    }

    public void onRefresh(ApplicationContext context) {
        initStrategies(context);
    }

    private void initStrategies(ApplicationContext context) {
        initHandlerNames(context);
        initInterceptors(context);
        initHandlerMappings(context);
        initHandlerAdapters();
    }

    private void initInterceptors(ApplicationContext context) {
        String[] interceptorsNames = context.getBeanNamesForType(HandlerInterceptor.class);
        handlerInterceptors = new ArrayList<>();
        for (String interceptorsName : interceptorsNames)
            handlerInterceptors.add((HandlerInterceptor) context.getBean(interceptorsName));
    }

    private void initHandlerNames(ApplicationContext context) {
        this.reqHandlerNames = context.getBeanNamesForAnnotation(io.norland.annotations.ReqController.class);
    }

    private void initHandlerMappings(ApplicationContext context) {
        this.reqHandlerMappings = new ArrayList<>();
        for (String protoHandlerName : reqHandlerNames) {
            Object delegate = context.getBean(protoHandlerName);
            MethodNameResolver methodNameResolver = new MethodNameResolver();
            ReqController controller = new SimpleReqController(delegate, methodNameResolver);
            Method[] methods = delegate.getClass().getDeclaredMethods();
            for (Method method : methods) {
                ReqMapping protoMapping = method.getAnnotation(ReqMapping.class);
                if (protoMapping != null) {
                    String[] supportProto = protoMapping.value();
                    for (String proto : supportProto) {
                        HandlerMapping mapping = new HandlerMapping(proto, controller, handlerInterceptors);
                        methodNameResolver.add(proto, method.getName());
                        if (!isDoubleDeclared(mapping))
                            reqHandlerMappings.add(mapping);
                        else
                            throw new RuntimeException("double declared requestProtocol:" + proto);
                    }
                }

            }
        }
        Collections.sort(reqHandlerMappings);
    }

    private void initHandlerAdapters() {
        this.reqHandlerAdapters = new ArrayList<>();
        reqHandlerAdapters.add(new ReqHandlerAdapter());
    }

    private boolean isDoubleDeclared(HandlerMapping mapping) {
        long sum = this.reqHandlerMappings.stream().filter(handlerMapping -> handlerMapping.equals(mapping)).count();
        return sum != 0L;
    }

    public Object dispatch(AbstractWrapper request, Channel channel) throws Exception {
        HandlerExecutionChain mappedHandler = getHandler(request);
        if (mappedHandler == null)
            throw new RuntimeException("This protocol may not Uplink protocol there is no handler");
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getController());
        if (ha == null)
            throw new RuntimeException("no Adapter");
        if (!mappedHandler.applyPreHandle(request)) {
            return null;
        }
        ActionAndModel model = (ActionAndModel) ha.handle(request,
                mappedHandler.getController(),
                channel);
        mappedHandler.applyPostHandle(request, model);
        return processDispatchResult(model);
    }

    private Object processDispatchResult(ActionAndModel model) throws Exception {
        if (Actions.NO_RESPONSE.equals(model.getAction())
                || model.getValue() == null) {
            return null;
        } else {
            return model.getValue();
        }
    }

    private HandlerExecutionChain getHandler(AbstractWrapper request) {
        int index = Collections.binarySearch(reqHandlerMappings,
                new HandlerMapping(request.requestProtocol()));
        if (index >= 0) {
            HandlerExecutionChain handler = (HandlerExecutionChain) reqHandlerMappings.get(index).getHandlerExecutionChain();
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private HandlerAdapter getHandlerAdapter(Object handler) {
        for (HandlerAdapter ha : this.reqHandlerAdapters) {
            if (ha.supports(handler)) {
                return ha;
            }
        }
        return null;
    }
}
