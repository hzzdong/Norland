package io.norland.dispatcher.interceptor;

import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;

public interface PostFunction {
    void apply(AbstractWrapper req, ActionAndModel actionAndModel);
}
