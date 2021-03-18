package com.tonytaotao.rpc.cluster;


import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.extension.Scope;
import com.tonytaotao.rpc.core.reference.Reference;

import java.util.List;

@SPI(scope = Scope.PROTOTYPE)
public interface LoadBalance<T> {

    void setReferences(List<Reference<T>> references);

    Reference<T> select(Request request);
}
