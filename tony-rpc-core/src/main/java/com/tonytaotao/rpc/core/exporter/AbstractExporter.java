package com.tonytaotao.rpc.core.exporter;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.provider.Provider;

public abstract class AbstractExporter<T> implements Exporter<T> {

    protected Provider<T> provider;
    protected URL url;

    public AbstractExporter(Provider<T> provider, URL url) {
        this.url = url;
        this.provider = provider;
    }

    @Override
    public String desc() {
        return "[" + this.getClass().getName() + "] url=" + url;
    }

    @Override
    public Provider<T> getProvider() {
        return provider;
    }

    @Override
    public URL getUrl() {
        return url;
    }
}
