package com.tonytaotao.rpc.core;

import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;

public interface Caller<T> extends Node {

    Class<T> getInterface();

    Response call(Request request);

}
