package io.norland.dispatcher;

import io.norland.dispatcher.interceptor.HandlerInterceptor;
import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HandlerExecutionChain {
    private HandlerInterceptor[] interceptors;
    private List<HandlerInterceptor> interceptorList;

    private final Object controller;

    public HandlerExecutionChain(Object handler) {
        this.controller = handler;
    }

    public Object getController() {
        return controller;
    }

    public HandlerInterceptor[] getInterceptors() {
        if (interceptors == null && interceptorList != null) {
            interceptors = interceptorList.toArray(new HandlerInterceptor[interceptorList.size()]);
        }
        return interceptors;
    }

    public boolean applyPreHandle(AbstractWrapper request) throws Exception {
        if (getInterceptors() != null) {
            for (int i = 0; i < getInterceptors().length; i++) {
                HandlerInterceptor interceptor = getInterceptors()[i];
                if (!interceptor.preHandle(request, controller)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void applyPostHandle(AbstractWrapper request, ActionAndModel actionAndModel) throws Exception {
        if (getInterceptors() == null) {
            return;
        }
        for (int i = getInterceptors().length - 1; i >= 0; i--) {
            HandlerInterceptor interceptor = getInterceptors()[i];
            interceptor.postHandle(request, controller, actionAndModel);
        }
    }

    public void addInterceptor(HandlerInterceptor interceptor) {
        initInterceptorList();
        this.interceptorList.add(interceptor);
    }

    public void addInterceptors(HandlerInterceptor[] interceptors) {
        if (interceptors != null) {
            initInterceptorList();
            this.interceptorList.addAll(Arrays.asList(interceptors));
        }
    }

    private void initInterceptorList() {
        if (this.interceptorList == null) {
            this.interceptorList = new ArrayList<>();
        }
        if (this.interceptors != null) {
            this.interceptorList.addAll(Arrays.asList(this.interceptors));
            this.interceptors = null;
        }
    }
}

