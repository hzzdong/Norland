package io.norland.dispatcher;

import io.norland.proto.AbstractWrapper;

import java.util.HashMap;
import java.util.Map;

public class MethodNameResolver {
    private final Map<String, String> protoNameToMethodName = new HashMap<>();

    public String getHandlerMethodName(AbstractWrapper request) {
        return protoNameToMethodName.get(request.requestProtocol());
    }

    public void add(String protoName, String methodName) {
        protoNameToMethodName.put(protoName, methodName);
    }
}
