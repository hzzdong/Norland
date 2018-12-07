package io.norland.dispatcher;

import io.netty.channel.Channel;
import io.norland.annotations.ReqMapping;
import io.norland.proto.AbstractWrapper;
import io.norland.response.ActionAndModel;
import io.norland.util.SharedParamUtil;
import io.norland.server.SharedParam;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SimpleReqController implements ReqController {


    private final Map<String, Method> handlerMethodMap = new HashMap<>();
    private Object delegate;
    private MethodNameResolver methodNameResolver;

    public SimpleReqController(Object delegate, MethodNameResolver methodNameResolver) {
        this.methodNameResolver = methodNameResolver;
        setDelegate(delegate);
    }

    public final void setDelegate(Object delegate) {
        Assert.notNull(delegate, "Delegate must not be null");
        this.delegate = delegate;
        registerHandlerMethods(this.delegate);
        // There must be SOME controller methods.
        if (this.handlerMethodMap.isEmpty()) {
            throw new IllegalStateException("No controller methods in class [" + this.delegate.getClass() + "]");
        }
    }

    private void registerHandlerMethods(Object delegate) {
        this.handlerMethodMap.clear();
        Method[] methods = delegate.getClass().getMethods();
        for (Method method : methods) {
            if (isHandlerMethod(method)) {
                registerHandlerMethod(method);
            }
        }
    }

    private boolean isHandlerMethod(Method method) {
        Class<?> returnType = method.getReturnType();
        if (ActionAndModel.class.equals(returnType)
                || void.class.equals(returnType)) {
            ReqMapping protoMapping = method.getDeclaredAnnotation(ReqMapping.class);
            if (protoMapping != null) {
                String[] protoNames = protoMapping.value();
                return protoNames.length != 0;
            }
        }
        return false;
    }

    private void registerHandlerMethod(Method method) {
        this.handlerMethodMap.put(method.getName(), method);
    }

    private ActionAndModel
    invokeNamedMethod(String methodName,
                      AbstractWrapper request,
                      Channel channel) throws Exception {
        try {
            Method method = this.handlerMethodMap.get(methodName);
            List<Object> params = new ArrayList<>(4);
            paramsMatcher(request, params, method, channel);
            Object returnValue = method.invoke(this.delegate, params.toArray());
            if (returnValue instanceof ActionAndModel) {
                return (ActionAndModel) returnValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ActionAndModel.noResponseModel();
    }

    private void paramsMatcher(AbstractWrapper request,
                               List<Object> params,
                               Method method,
                               Channel channel) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0)
            return;
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = discoverer.getParameterNames(method);
        assert paramNames != null;
        for (int i = 0; i < paramTypes.length; i++) {
            Class paramType = paramTypes[i];
            String paramName = paramNames[i];
            if (AbstractWrapper.class.isAssignableFrom(paramType)) {
                params.add(request);
                continue;
            }
            if (String.class == paramType && paramName.equals("serialNo")) {
                params.add(request.getTerminalSerialNo());
                continue;
            }
            //SharedParam存储在channel中，同一连接的不同消息之间可通过此类共享数据
            if (SharedParam.class == paramType) {
                SharedParam sharedParam = SharedParamUtil.getSharedParamByChannel(channel);
                if (sharedParam == null) {
                    sharedParam = new SharedParam();
                    SharedParamUtil.setSharedParam(channel, sharedParam);
                }
                params.add(sharedParam);
                continue;
            }
            //相同类型的参数的数量大于一时使用参数名匹配
            //否则直接使用参数类型匹配
            Object opt = findOptionalParamValue(paramType, paramName, request);
            if (opt != null) {
                params.add(opt);
                continue;
            }
            if (paramType.isPrimitive()) {
                throw new IllegalArgumentException(
                        "default value don't support primitive type");
            }
            params.add(null);
        }
    }

    private Object findOptionalParamValue(Class paramType, String paramName, AbstractWrapper request) {
        Map<String, Object> opts = new HashMap<>(4);
        Field[] fields = request.getClass().getDeclaredFields();//获取属性列表
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                continue;//常量直接拿
            }
            field.setAccessible(true);
            try {
                Object value = field.get(request);
                if (value != null && paramType.equals(value.getClass())) {
                    opts.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } finally {
                field.setAccessible(false);
            }
        }
        if (opts.size() == 1) {
            return opts.entrySet().iterator().next().getValue();
        }
        if (opts.size() > 1) {
            return opts.get(paramName);
        }
        return null;
    }

    private ActionAndModel
    handleRequestInternal(AbstractWrapper proto,
                          Channel channel) throws Exception {
        String methodName = methodNameResolver.getHandlerMethodName(proto);
        return invokeNamedMethod(methodName, proto, channel);
    }

    @Override
    public ActionAndModel handleRequest(AbstractWrapper request,
                                        Object handler,
                                        Channel channel) throws Exception {
        return handleRequestInternal(request, channel);
    }
}
