package io.norland.dispatcher;

import io.norland.dispatcher.interceptor.HandlerInterceptor;

import java.util.List;
import java.util.Objects;

public class HandlerMapping implements Comparable<HandlerMapping> {
    private String protoName;
    private Object handlerExecutionChain;

    public HandlerMapping(String reqProtoName) {
        this.protoName = reqProtoName;
    }

    public HandlerMapping(String proto, Object controller, List<HandlerInterceptor> handlerInterceptors) {
        protoName = proto;
        HandlerExecutionChain chain = new HandlerExecutionChain(controller);
        for (HandlerInterceptor interceptor : handlerInterceptors) {
            if (interceptor.supports(proto))
                chain.addInterceptor(interceptor);
        }
        handlerExecutionChain = chain;
    }

    /**
     * 只比较protoName
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(protoName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof HandlerMapping)
            return obj.hashCode() == this.hashCode();
        return false;
    }

    @Override
    public int compareTo(HandlerMapping reqHandlerMapping) {
        if (reqHandlerMapping == null) {
            return 1;
        }
        return getProtoName().compareTo(reqHandlerMapping.getProtoName());
    }

    public String getProtoName() {
        return protoName;
    }

    public void setProtoName(String protoName) {
        this.protoName = protoName;
    }

    public Object getHandlerExecutionChain() {
        return handlerExecutionChain;
    }

    public void setHandlerExecutionChain(Object handlerExecutionChain) {
        this.handlerExecutionChain = handlerExecutionChain;
    }
}
