package com.tonytaotao.rpc.registry;

import com.tonytaotao.rpc.common.URL;

import java.util.List;

public interface NotifyListener {

    void notify(URL registryUrl, List<URL> urls);
}
