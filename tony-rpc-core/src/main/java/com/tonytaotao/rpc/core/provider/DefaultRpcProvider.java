package com.tonytaotao.rpc.core.provider;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.exception.BusinessRpcException;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.response.Response;

import java.lang.reflect.Method;

public class DefaultRpcProvider<T> implements Provider<T> {

    private T proxyImpl;
    private Class<T> clazz;
    private URL url;
    private boolean available = false;

    public DefaultRpcProvider(T proxyImpl, URL url, Class<T> clazz) {
        this.proxyImpl = proxyImpl;
        this.url = url;
        this.clazz = clazz;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Class<T> getInterface() {
        return clazz;
    }

    @Override
    public Response call(Request request) {
        Response response = invoke(request);

        return response;
    }

    @Override
    public void init() {
        available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;
    }

    @Override
    public String desc() {
        return "[" + this.getClass().getName() + "] url=" + url;
    }

    private Response invoke(Request request) {

        DefaultResponse response = new DefaultResponse();
        response.setRequestId(request.getRequestId());

        Method method = lookup(request);
        if (method == null) {
            FrameworkRpcException exception = new FrameworkRpcException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName());

            response.setException(exception);
            return response;
        }
        try {
            method.setAccessible(true);
            Object result = method.invoke(proxyImpl, request.getArguments());
            response.setResult(result);
        } catch (Exception e) {
            response.setException(new BusinessRpcException("invoke failure", e));
        }
        return response;
    }

    private Method lookup(Request request) {
        try {
            return clazz.getMethod(request.getMethodName(), request.getParameterTypes());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

}
