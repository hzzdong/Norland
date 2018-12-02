package io.norland.server;

import java.util.HashMap;
import java.util.Map;

public class SharedParam {
    private Map<String, Object> sharedParams = new HashMap<>(6);

    public SharedParam() {
    }

    public void setParam(String key, Object value) {
        sharedParams.put(key, value);
    }

    public Object getAndRemove(String key) {
        return sharedParams.remove(key);
    }

    public Object get(String key) {
        return sharedParams.get(key);
    }

    public void clear() {
        sharedParams.clear();
    }
}
