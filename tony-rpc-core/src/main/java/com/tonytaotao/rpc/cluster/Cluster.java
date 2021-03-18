package com.tonytaotao.rpc.cluster;

import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.extension.Scope;
import com.tonytaotao.rpc.core.Caller;
import com.tonytaotao.rpc.core.reference.Reference;

import java.util.List;

@SPI(scope = Scope.PROTOTYPE)
public interface Cluster<T> extends Caller<T> {

    void setLoadBalance(LoadBalance<T> loadBalance);

    void setHaStrategy(HaStrategy<T> haStrategy);

    List<Reference<T>> getReferences();

    LoadBalance<T> getLoadBalance();
}
