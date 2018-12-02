package io.norland.dispatcher.interceptor;

import io.norland.proto.AbstractWrapper;

public interface PreFunction {
    boolean apply(AbstractWrapper req);
}
