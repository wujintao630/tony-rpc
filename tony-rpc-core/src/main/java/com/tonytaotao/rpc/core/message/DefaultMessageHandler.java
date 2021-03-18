package com.tonytaotao.rpc.core.message;

import com.tonytaotao.rpc.core.provider.Provider;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.common.exception.BusinessRpcException;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.common.util.FrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultMessageHandler implements MessageHandler {

    private ConcurrentHashMap<String, Provider<?>> providers = new ConcurrentHashMap<>();

    public DefaultMessageHandler(Provider<?> provider) {
        addProvider(provider);
    }

    @Override
    public Response handle(Request request) {

        String serviceKey = FrameworkUtils.getServiceKey(request);

        Provider<?> provider = providers.get(serviceKey);

        if (provider == null) {
            log.error(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey=" + serviceKey);
            FrameworkRpcException exception = new FrameworkRpcException(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey=" + serviceKey );

            DefaultResponse response = new DefaultResponse();
            response.setException(exception);
            return response;
        }

        return call(request, provider);
    }

    protected Response call(Request request, Provider<?> provider) {
        try {
            return provider.call(request);
        } catch (Exception e) {
            DefaultResponse response = new DefaultResponse();
            response.setException(new BusinessRpcException("provider call process error", e));
            return response;
        }
    }

    public synchronized void addProvider(Provider<?> provider) {
        String serviceKey = FrameworkUtils.getServiceKey(provider.getUrl());
        if (providers.containsKey(serviceKey)) {
            throw new FrameworkRpcException("provider alread exist: " + serviceKey);
        }
        providers.put(serviceKey, provider);
        log.info("RequestRouter addProvider: url=" + provider.getUrl());
    }

    public synchronized void removeProvider(Provider<?> provider) {
        String serviceKey = FrameworkUtils.getServiceKey(provider.getUrl());
        providers.remove(serviceKey);
        log.info("RequestRouter removeProvider: url=" + provider.getUrl());
    }
}
