package com.tonytaotao.rpc.netty;

import com.tonytaotao.rpc.common.URL;

import java.net.InetSocketAddress;

public interface Endpoint {

    /**
     * 获取本地地址
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取远程地址
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 获取url
     * @return
     */
    URL getUrl();

    /**
     * 打开终端
     * @return
     */
    boolean open();

    /**
     * 是否可用
     * @return
     */
    boolean isAvailable();

    /**
     * 是否关闭状态
     * @return
     */
    boolean isClosed();

    /**
     * 关闭终端
     */
    void close();

    /**
     * 指定时间内关闭终端
     * @param timeout
     */
    void close(int timeout);
}
