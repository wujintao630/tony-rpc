package com.tonytaotao.rpc.cluster;

import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.extension.Scope;

@SPI(scope = Scope.PROTOTYPE)
public interface HaStrategy<T> {

    Response call(Request request, LoadBalance loadBalance);
}
