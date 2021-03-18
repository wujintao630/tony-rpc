package com.tonytaotao.rpc.registry;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.extension.Scope;

/**
 * 服务注册中心接口
 */
@SPI(scope = Scope.SINGLETON)
public interface SpiRegistry {

    ServiceCommon getRegistry(URL url);
}
