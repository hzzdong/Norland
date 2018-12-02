package io.norland.dispatcher.interceptor;

import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;

public interface HandlerInterceptor {

    boolean supports(String proto);

    boolean preHandle(AbstractWrapper request, Object handler)
            throws Exception;

    void postHandle(AbstractWrapper request,
                    Object handler,
                    ActionAndModel actionAndModel)
            throws Exception;
}
