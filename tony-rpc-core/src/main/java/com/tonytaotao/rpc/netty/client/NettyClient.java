package com.tonytaotao.rpc.netty.client;

import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.response.future.ResponseFuture;
import com.tonytaotao.rpc.common.exception.TransportRpcException;
import com.tonytaotao.rpc.netty.Endpoint;

public interface NettyClient extends Endpoint {

    Response invokeSync(final Request request) throws InterruptedException, TransportRpcException;

    ResponseFuture invokeAsync(final Request request) throws InterruptedException, TransportRpcException;

    void invokeOneway(final Request request) throws InterruptedException, TransportRpcException;

}
