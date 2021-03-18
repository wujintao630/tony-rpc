package com.tonytaotao.rpc.common.exception;

public class TransportRpcException extends AbstractRpcException {

    private static final long serialVersionUID = 1391824218667687554L;

    public TransportRpcException() {
    }

    public TransportRpcException(String message) {
        super(message);
    }

    public TransportRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
