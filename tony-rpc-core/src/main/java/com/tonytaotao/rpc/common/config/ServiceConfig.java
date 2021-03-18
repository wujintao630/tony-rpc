package com.tonytaotao.rpc.common.config;


import com.google.common.collect.ArrayListMultimap;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.core.exporter.Exporter;
import com.tonytaotao.rpc.core.config.ConfigHandler;
import com.tonytaotao.rpc.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServiceConfig<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = -6784362174923673740L;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile boolean exported = false;
    private List<Exporter<T>> exporters = new CopyOnWriteArrayList<>();
    private ArrayListMultimap<URL, URL> registeredUrls = ArrayListMultimap.create();
    private Class<T> interfaceClass;
    private T ref;

    protected synchronized void export() {
        if (exported) {
            logger.warn(String.format("%s has already been exported, so ignore the export request!", interfaceName));
            return;
        }

        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        try {
            interfaceClass = (Class<T>) Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if(!interfaceClass.isAssignableFrom(ref.getClass())) {
            throw new IllegalArgumentException(ref.getClass() +" is not "+interfaceClass+" sub class!");
        }

        if (getRegistries() == null || getRegistries().isEmpty()) {
            throw new IllegalStateException("Should set registry config for service:" + interfaceClass.getName());
        }

        List<URL> registryUrls = loadRegistryUrls();
        if (registryUrls == null || registryUrls.size() == 0) {
            throw new IllegalStateException("Should set registry config for service:" + interfaceClass.getName());
        }

        for(ProtocolConfig protocol : protocols) {

            doExport(protocol, registryUrls);
        }
        exported = true;
    }

    private void doExport(ProtocolConfig protocol, List<URL> registryUrls) {
        String protocolName = protocol.getName();
        if (protocolName == null || protocolName.length() == 0) {
            protocolName = UrlParamEnum.protocol.getDefaultValue();
        }

        Integer port = getProtocolPort(protocol);
        String hostAddress = getLocalHostAddress(protocol);

        Map<String, String> map = new HashMap<String, String>();
        map.put(UrlParamEnum.application.getName(), StringUtils.isNotEmpty(application.getName()) ? application.getName() : UrlParamEnum.application.getDefaultValue());
        map.put(UrlParamEnum.version.getName(), StringUtils.isNotEmpty(version) ? version : UrlParamEnum.version.getDefaultValue());
        map.put(UrlParamEnum.group.getName(), StringUtils.isNotEmpty(group) ? group : UrlParamEnum.group.getDefaultValue());
        map.put(UrlParamEnum.serialization.getName(), StringUtils.isNotEmpty(protocol.getSerialization()) ? protocol.getSerialization(): UrlParamEnum.serialization.getDefaultValue());
        map.put(UrlParamEnum.requestTimeout.getName(), timeout!=null ? timeout.toString() : UrlParamEnum.requestTimeout.getDefaultValue());
        map.put(UrlParamEnum.side.getName(), Constants.PROVIDER);
        map.put(UrlParamEnum.timestamp.getName(), String.valueOf(System.currentTimeMillis()));

        URL serviceUrl = new URL(protocolName, hostAddress, port, interfaceClass.getName(), map);

        for(URL ru : registryUrls) {
            registeredUrls.put(serviceUrl, ru);
        }

        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(Constants.DEFAULT_VALUE);
        exporters.add(configHandler.export(interfaceClass, ref, serviceUrl, registryUrls));
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public boolean isExported() {
        return exported;
    }

    protected void destroy0() throws Exception {

        if(!exported) {
            return;
        }

        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(Constants.DEFAULT_VALUE);
        configHandler.unexport(exporters, registeredUrls);

        exporters.clear();
        registeredUrls.clear();
    }

}
