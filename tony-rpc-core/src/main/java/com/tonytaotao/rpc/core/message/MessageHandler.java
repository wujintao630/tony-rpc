package com.tonytaotao.rpc.core.message;

import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;

public interface MessageHandler {

    Response handle(Request request);

}
