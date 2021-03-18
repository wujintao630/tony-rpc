package com.tonytaotao.rpc.core.reference;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.netty.client.NettyClient;
import com.tonytaotao.rpc.netty.client.DefaultNettyClient;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DefaultRpcReference<T> implements Reference<T> {

    private NettyClient client;

    private Class<T> clazz;
    private URL url;
    private URL serviceUrl;

    private AtomicInteger activeCounter = new AtomicInteger(0);

    public DefaultRpcReference(Class<T> clazz, URL url, URL serviceUrl) {
        this.clazz = clazz;
        this.url = url;
        this.serviceUrl = serviceUrl;
        this.client = new DefaultNettyClient(serviceUrl);
    }

    @Override
    public URL getServiceUrl() {
        return serviceUrl;
    }

    @Override
    public Class<T> getInterface() {
        return clazz;
    }

    @Override
    public Response call(Request request) {
        if (!isAvailable()) {
            throw new FrameworkRpcException(this.getClass().getName() + " call Error: node is not available, url=" + url.getUri());
        }

        incrActiveCount(request);
        Response response = null;
        try {
            response = doCall(request);
            return response;
        } finally {
            decrActiveCount(request, response);
        }

    }

    @Override
    public int activeCount() {
        return activeCounter.get();
    }

    private Response doCall(Request request) {
        try {
            return client.invokeSync(request);
        } catch (Exception e) {
            throw new FrameworkRpcException("invoke exception", e);
        }
    }

    private void decrActiveCount(Request request, Response response) {
        activeCounter.decrementAndGet();
    }

    private void incrActiveCount(Request request) {
        activeCounter.incrementAndGet();
    }

    @Override
    public String desc() {
        return "[" + this.getClass().getName() + "] url=" + url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void init() {
        this.client.open();
    }

    @Override
    public void destroy() {
        try{
            client.close();
        } catch (Exception e){
            log.error("reference destroy error", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable();
    }
}
