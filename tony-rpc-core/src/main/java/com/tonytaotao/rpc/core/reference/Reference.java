package com.tonytaotao.rpc.core.reference;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.Caller;

public interface Reference<T> extends Caller<T> {

    /**
     * 当前Reference的调用次数
     * @return
     */
    int activeCount();

    URL getServiceUrl();
}
