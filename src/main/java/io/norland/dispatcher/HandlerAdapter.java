package io.norland.dispatcher;

public interface HandlerAdapter {

    boolean supports(Object handler);

    Object handle(Object value,
                  Object handler,
                  Object... moreParams) throws Exception;
}
