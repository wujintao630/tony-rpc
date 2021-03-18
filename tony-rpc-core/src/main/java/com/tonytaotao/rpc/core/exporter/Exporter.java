package com.tonytaotao.rpc.core.exporter;

import com.tonytaotao.rpc.core.provider.Provider;
import com.tonytaotao.rpc.core.Node;

public interface Exporter<T> extends Node {

    Provider<T> getProvider();

    void unexport();
}
