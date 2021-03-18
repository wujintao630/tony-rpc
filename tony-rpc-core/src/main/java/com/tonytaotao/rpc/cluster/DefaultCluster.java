package com.tonytaotao.rpc.cluster;


import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.common.exception.BusinessRpcException;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.common.exception.ServiceRpcException;
import com.tonytaotao.rpc.protocol.ProtocolFilterWrapper;
import com.tonytaotao.rpc.registry.NotifyListener;
import com.tonytaotao.rpc.registry.ServiceCommon;
import com.tonytaotao.rpc.core.reference.Reference;
import com.tonytaotao.rpc.protocol.Protocol;
import com.tonytaotao.rpc.registry.SpiRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultCluster<T> implements Cluster<T>, NotifyListener {

    private List<URL> registryUrls;

    private URL url;

    private Class<T> interfaceClass;

    private Protocol protocol;

    private HaStrategy<T> haStrategy;

    private LoadBalance<T> loadBalance;

    private volatile List<Reference<T>> references;

    private ConcurrentHashMap<URL, List<Reference<T>>> registryReferences = new ConcurrentHashMap<>();

    private volatile boolean available;

    public DefaultCluster(Class<T> interfaceClass, URL url, List<URL> registryUrls) {
        this.registryUrls = registryUrls;
        this.interfaceClass = interfaceClass;
        this.url = url;
        protocol = new ProtocolFilterWrapper(ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(url.getProtocol()));
    }

    @Override
    public void init() {

        URL subscribeUrl = url.clone0();
        for (URL ru : registryUrls) {

            ServiceCommon registry = getRegistry(ru);
            try {
                notify(ru, registry.discover(subscribeUrl));
            } catch (Exception e) {
                log.error(String.format("Cluster init discover for the reference:%s, registry:%s", this.url, ru), e);
            }
            // client 注册自己，同时订阅service列表
            registry.subscribe(subscribeUrl, this);
        }

        log.info("Cluster init over, url:{}, references size:{}", url, references!=null ? references.size():0);
        boolean check = Boolean.parseBoolean(url.getStrParameterByEnum(UrlParamEnum.check));
        if(CollectionUtils.isEmpty(references)) {
            log.warn(String.format("Cluster No service urls for the reference:%s, registries:%s", this.url, registryUrls));
            if(check) {
                throw new FrameworkRpcException(String.format("Cluster No service urls for the reference:%s, registries:%s", this.url, registryUrls));
            }
        }
        available = true;
    }

    @Override
    public void destroy() {
        URL subscribeUrl = url.clone0();
        for (URL ru : registryUrls) {
            try {
                ServiceCommon registry = getRegistry(ru);
                registry.unsubscribe(subscribeUrl, this);
                registry.unregister(url);
            } catch (Exception e) {
                log.warn(String.format("Unregister or unsubscribe false for url (%s), registry= %s", url, ru), e);
            }
        }
        if(references!=null) {
            for (Reference<T> reference : this.references) {
                reference.destroy();
            }
        }
        available = false;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String desc() {
        return null;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }

    @Override
    public Response call(Request request) {
        if(available) {
            try {
                return haStrategy.call(request, loadBalance);
            } catch (Exception e) {
                if (e instanceof BusinessRpcException) {
                    throw (RuntimeException) e;
                }
                return buildErrorResponse(request, e);
            }
        }
        return buildErrorResponse(request, new ServiceRpcException("service not available"));
    }

    private Response buildErrorResponse(Request request, Exception motanException) {
        DefaultResponse rs = new DefaultResponse();
        rs.setException(motanException);
        rs.setRequestId(request.getRequestId());
        return rs;
    }

    @Override
    public void setLoadBalance(LoadBalance<T> loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public void setHaStrategy(HaStrategy<T> haStrategy) {
        this.haStrategy = haStrategy;
    }

    @Override
    public List<Reference<T>> getReferences() {
        return references;
    }

    @Override
    public LoadBalance<T> getLoadBalance() {
        return loadBalance;
    }

    @Override
    public synchronized void notify(URL registryUrl, List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            log.warn("Cluster config change notify, urls is empty: registry={} service={} urls=[]", registryUrl.getUri(), url, urls);
            return;
        }

        log.info("Cluster config change notify: registry={} service={} urls={}", registryUrl.getUri(), url, urls);

        List<Reference<T>> newReferences = new ArrayList<>();
        for (URL u : urls) {
            if (!u.canServe(url)) {
                continue;
            }
            Reference<T> reference = getExistingReference(u, registryReferences.get(registryUrl));
            if (reference == null) {
                URL referenceURL = u.clone0();
                reference = protocol.refer(interfaceClass, referenceURL, u);
            }
            if (reference != null) {
                newReferences.add(reference);
            }
        }
        registryReferences.put(registryUrl, newReferences);

        refresh();
    }

    private void refresh() {
        List<Reference<T>> references = new ArrayList<>();
        for (List<Reference<T>> refs : registryReferences.values()) {
            references.addAll(refs);
        }
        this.references = references;
        this.loadBalance.setReferences(references);
    }

    private Reference<T> getExistingReference(URL url, List<Reference<T>> referers) {
        if (referers == null) {
            return null;
        }
        for (Reference<T> r : referers) {
            if (url.equals(r.getUrl()) || url.equals(r.getServiceUrl())) {
                return r;
            }
        }
        return null;
    }

    private ServiceCommon getRegistry(URL registryUrl) {
        SpiRegistry registryFactory = ExtensionLoader.getExtensionLoader(SpiRegistry.class).getExtension(registryUrl.getProtocol());
        if (registryFactory == null) {
            throw new FrameworkRpcException("register error! Could not find extension for registry protocol:" + registryUrl.getProtocol()
                    + ", make sure registry module for " + registryUrl.getProtocol() + " is in classpath!");
        }
        return registryFactory.getRegistry(registryUrl);
    }
}
