package com.tonytaotao.rpc.registry;

import com.tonytaotao.rpc.common.URL;

import java.util.List;

/**
 * 服务订阅与发现
 * @author tony
 */
public interface ServiceDiscovery {

    /**
     * 订阅服务
     * @param url
     * @param listener
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅
     * @param url
     * @param listener
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * 在注册中心查找符合条件的URL
     * @param url
     * @return
     */
    List<URL> discover(URL url) throws Exception;
}
