package com.tonytaotao.rpc.cluster.ha;


import com.tonytaotao.rpc.cluster.HaStrategy;
import com.tonytaotao.rpc.cluster.LoadBalance;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.reference.Reference;

public class FailfastHaStrategy<T> implements HaStrategy<T> {

    @Override
    public Response call(Request request, LoadBalance loadBalance) {
        Reference<T> reference = loadBalance.select(request);
        return reference.call(request);
    }
}
