package com.tonytaotao.rpc.protocol;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.core.exporter.Exporter;
import com.tonytaotao.rpc.core.provider.Provider;
import com.tonytaotao.rpc.core.reference.Reference;
import com.tonytaotao.rpc.common.util.FrameworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractProtocol implements Protocol {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ConcurrentHashMap<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    @Override
    public <T> Reference<T> refer(Class<T> clz, URL url, URL serviceUrl) {
        if (url == null) {
            throw new FrameworkRpcException(this.getClass().getSimpleName() + " refer Error: url is null");
        }
        if (clz == null) {
            throw new FrameworkRpcException(this.getClass().getSimpleName() + " refer Error: class is null, url=" + url);
        }
        Reference<T> reference = createReference(clz, url, serviceUrl);
        reference.init();

        logger.info(this.getClass().getSimpleName() + " refer service:{} success url:{}", clz.getName(), url);
        return reference;
    }

    @Override
    public <T> Exporter<T> export(Provider<T> provider, URL url) {
        if (url == null) {
            throw new FrameworkRpcException(this.getClass().getSimpleName() + " export Error: url is null");
        }

        if (provider == null) {
            throw new FrameworkRpcException(this.getClass().getSimpleName() + " export Error: provider is null, url=" + url);
        }

        String protocolKey = FrameworkUtils.getProtocolKey(url);

        synchronized (exporterMap) {
            Exporter<T> exporter = (Exporter<T>) exporterMap.get(protocolKey);

            if (exporter != null) {
                throw new FrameworkRpcException(this.getClass().getSimpleName() + " export Error: service already exist, url=" + url);
            }

            exporter = createExporter(provider, url);
            exporter.init();
            exporterMap.put(protocolKey, exporter);
            logger.info(this.getClass().getSimpleName() + " export success: url=" + url);
            return exporter;
        }
    }

    protected abstract <T> Reference<T> createReference(Class<T> clz, URL url, URL serviceUrl);

    protected abstract <T> Exporter<T> createExporter(Provider<T> provider, URL url);
}
