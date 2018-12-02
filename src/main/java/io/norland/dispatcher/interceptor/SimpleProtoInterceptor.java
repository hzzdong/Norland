package io.norland.dispatcher.interceptor;

import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public class SimpleProtoInterceptor implements HandlerInterceptor {
    @Singular
    private List<String> supportProtoNames;
    private PreFunction preHandle;
    private PostFunction postHandle;

    @Override
    public boolean supports(String proto) {
        return "all".equalsIgnoreCase(proto) || supportProtoNames.contains(proto);
    }

    @Override
    public boolean preHandle(AbstractWrapper req, Object handler) throws Exception {
        return preHandle == null || preHandle.apply(req);
    }

    @Override
    public void postHandle(AbstractWrapper req, Object handler, ActionAndModel actionAndModel) throws Exception {
        if (postHandle != null)
            postHandle.apply(req, actionAndModel);
    }
}
