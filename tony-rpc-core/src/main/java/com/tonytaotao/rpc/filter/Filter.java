package com.tonytaotao.rpc.filter;

import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.Caller;

@SPI
public interface Filter {

    Response filter(Caller<?> caller, Request request);

}
