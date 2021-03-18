package com.tonytaotao.rpc.proxy;

import com.tonytaotao.rpc.core.extension.SPI;

import java.lang.reflect.InvocationHandler;

@SPI("jdk")
public interface ProxyFactory {

    <T> T getProxy(Class<T> clz, InvocationHandler invocationHandler);
}
