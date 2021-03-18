package com.tonytaotao.rpc.common.config;


import com.tonytaotao.rpc.cluster.Cluster;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.core.config.ConfigHandler;
import com.tonytaotao.rpc.common.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceConfig<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = 3259358868568571457L;
    private Class<T> interfaceClass;
    protected transient volatile T proxy;

    private transient volatile boolean initialized;
    private List<Cluster<T>> clusters;

    public T get() {
        if (proxy == null) {
            init();
        }
        return proxy;
    }

    private synchronized void init() {
        if (initialized) {
            return;
        }

        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<tonyrpc:reference interface=\"\" /> interface not allow null!");
        }
        try {
            interfaceClass = (Class<T>) Class.forName(interfaceName, true, Thread.currentThread()
                    .getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("reference class not found", e);
        }

        initProxy();

        initialized = true;
    }

    private void initProxy() {
        if(!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("<tonyrpc:reference interface=\"\" /> is not interface!");
        }

        List<URL> registryUrls = loadRegistryUrls();
        if (registryUrls == null || registryUrls.size() == 0) {
            throw new IllegalStateException("Should set registry config for reference:" + interfaceClass.getName());
        }

        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(Constants.DEFAULT_VALUE);

        clusters = new ArrayList<>(protocols.size());
        String proxyType = null;
        for(ProtocolConfig protocol : protocols) {

            Map<String, String> map = new HashMap<>();
            map.put(UrlParamEnum.application.getName(), StringUtils.isNotEmpty(application.getName()) ? application.getName() : UrlParamEnum.application.getDefaultValue());
            map.put(UrlParamEnum.serialization.getName(), StringUtils.isNotEmpty(protocol.getSerialization()) ? protocol.getSerialization(): UrlParamEnum.serialization.getDefaultValue());
            map.put(UrlParamEnum.version.getName(), StringUtils.isNotEmpty(version) ? version : UrlParamEnum.version.getDefaultValue());
            map.put(UrlParamEnum.group.getName(), StringUtils.isNotEmpty(group) ? group : UrlParamEnum.group.getDefaultValue());
            map.put(UrlParamEnum.side.getName(), Constants.CONSUMER);
            map.put(UrlParamEnum.requestTimeout.getName(), String.valueOf(getTimeout()));
            map.put(UrlParamEnum.timestamp.getName(), String.valueOf(System.currentTimeMillis()));
            map.put(UrlParamEnum.check.getName(), isCheck().toString());

            String hostAddress = getLocalHostAddress(protocol);
            Integer port = getProtocolPort(protocol);

            URL refUrl = new URL(protocol.getName(), hostAddress, port, interfaceClass.getName(), map);

            clusters.add(configHandler.buildCluster(interfaceClass, refUrl, registryUrls));

            proxyType = refUrl.getStrParameterByEnum(UrlParamEnum.proxyType);
        }

        this.proxy = configHandler.refer(interfaceClass, clusters, proxyType);
    }

    public T getProxy() {
        return proxy;
    }

    public void setProxy(T proxy) {
        this.proxy = proxy;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    protected void destroy0() throws Exception {
        if (clusters != null) {
            for (Cluster<T> cluster : clusters) {
                cluster.destroy();
            }
        }
        proxy = null;
    }
}
