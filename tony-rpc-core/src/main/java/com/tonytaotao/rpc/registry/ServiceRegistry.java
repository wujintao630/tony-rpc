package com.tonytaotao.rpc.registry;

import com.tonytaotao.rpc.common.URL;

/**
 * 服务注册
 * @author tony
 */
public interface ServiceRegistry {

    /**
     * 注册服务
     *
     * @param url
     */
    void register(URL url) throws Exception;

    /**
     * 取消注册
     *
     * @param url
     */
    void unregister(URL url) throws Exception;

}
