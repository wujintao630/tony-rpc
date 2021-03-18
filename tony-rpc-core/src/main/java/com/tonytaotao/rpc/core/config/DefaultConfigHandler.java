package com.tonytaotao.rpc.core.config;

import com.google.common.collect.ArrayListMultimap;
import com.tonytaotao.rpc.cluster.Cluster;
import com.tonytaotao.rpc.cluster.DefaultCluster;
import com.tonytaotao.rpc.cluster.HaStrategy;
import com.tonytaotao.rpc.cluster.LoadBalance;
import com.tonytaotao.rpc.core.exporter.Exporter;
import com.tonytaotao.rpc.core.provider.DefaultRpcProvider;
import com.tonytaotao.rpc.core.provider.Provider;
import com.tonytaotao.rpc.protocol.Protocol;
import com.tonytaotao.rpc.proxy.ProxyFactory;
import com.tonytaotao.rpc.registry.ServiceCommon;
import com.tonytaotao.rpc.registry.SpiRegistry;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.protocol.ProtocolFilterWrapper;
import com.tonytaotao.rpc.proxy.ReferenceInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DefaultConfigHandler implements ConfigHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public <T> Cluster<T> buildCluster(Class<T> interfaceClass, URL refUrl, List<URL> registryUrls) {
        DefaultCluster<T> cluster = new DefaultCluster(interfaceClass, refUrl, registryUrls);
        String loadBalanceName = refUrl.getStrParameterByEnum(UrlParamEnum.loadBalance);
        String haStrategyName = refUrl.getStrParameterByEnum(UrlParamEnum.haStrategy);
        LoadBalance<T> loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadBalanceName);
        HaStrategy<T> ha = ExtensionLoader.getExtensionLoader(HaStrategy.class).getExtension(haStrategyName);
        cluster.setLoadBalance(loadBalance);
        cluster.setHaStrategy(ha);

        cluster.init();
        return cluster;
    }

    @Override
    public <T> T refer(Class<T> interfaceClass, List<Cluster<T>> cluster, String proxyType) {
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension(proxyType);
        return (T) proxyFactory.getProxy(interfaceClass, new ReferenceInvocationHandler<>(interfaceClass, cluster));
    }

    @Override
    public <T> Exporter<T> export(Class<T> interfaceClass, T ref, URL serviceUrl, List<URL> registryUrls) {

        String protocolName = serviceUrl.getStrParameterByEnum(UrlParamEnum.protocol);
        Provider<T> provider = new DefaultRpcProvider<T>(ref, serviceUrl, interfaceClass);
        Protocol protocol = new ProtocolFilterWrapper(ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName));
        Exporter<T> exporter = protocol.export(provider, serviceUrl);

        // register service
        register(registryUrls, serviceUrl);

        return exporter;
    }

    @Override
    public <T> void unexport(List<Exporter<T>> exporters, ArrayListMultimap<URL, URL> registryUrls) {
        try {
            unRegister(registryUrls);
        } catch (Exception e){
            logger.warn("Exception when unexport registryUrls:{}", registryUrls);
        }

        for (Exporter<T> exporter : exporters) {
            try {
                exporter.unexport();
            } catch (Exception e) {
                logger.warn("Exception when unexport exporters:{}", exporters);
            }
        }
    }

    private void unRegister(ArrayListMultimap<URL, URL> registryUrls) {

        for (URL serviceUrl : registryUrls.keySet()) {
            for (URL url : registryUrls.get(serviceUrl)) {
                try {
                    SpiRegistry registryFactory = ExtensionLoader.getExtensionLoader(SpiRegistry.class).getExtension(url.getProtocol());
                    ServiceCommon registry = registryFactory.getRegistry(url);
                    registry.unregister(serviceUrl);
                } catch (Exception e) {
                    logger.warn(String.format("unregister url false:%s", url), e);
                }
            }
        }
    }

    private void register(List<URL> registryUrls, URL serviceUrl) {

        for (URL registryUrl : registryUrls) {
            // 根据check参数的设置，register失败可能会抛异常，上层应该知晓
            SpiRegistry registryFactory = ExtensionLoader.getExtensionLoader(SpiRegistry.class).getExtension(registryUrl.getProtocol());
            if (registryFactory == null) {
                throw new FrameworkRpcException("register error! Could not find extension for registry protocol:" + registryUrl.getProtocol()
                                + ", make sure registry module for " + registryUrl.getProtocol() + " is in classpath!");
            }
            try {
                ServiceCommon registry = registryFactory.getRegistry(registryUrl);
                registry.register(serviceUrl);
            } catch (Exception e) {
                throw new FrameworkRpcException("register error! Could not registry service:" + serviceUrl.getPath()
                        + " for " + registryUrl.getProtocol(), e);
            }
        }
    }
}
