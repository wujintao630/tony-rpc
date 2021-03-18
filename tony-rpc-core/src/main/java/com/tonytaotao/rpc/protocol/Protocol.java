package com.tonytaotao.rpc.protocol;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.exporter.Exporter;
import com.tonytaotao.rpc.core.provider.Provider;
import com.tonytaotao.rpc.core.reference.Reference;
import com.tonytaotao.rpc.common.Constants;

@SPI(value = Constants.FRAMEWORK_NAME)
public interface Protocol {

    <T> Reference<T> refer(Class<T> clz, URL url, URL serviceUrl);

    <T> Exporter<T> export(Provider<T> provider, URL url);

    void destroy();
}
