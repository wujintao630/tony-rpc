package com.tonytaotao.rpc.registry;

import com.tonytaotao.rpc.common.URL;

/**
 * 统一服务的注册与发现
 */
public interface ServiceCommon extends ServiceRegistry, ServiceDiscovery {

    URL getUrl();

    void close();
}
