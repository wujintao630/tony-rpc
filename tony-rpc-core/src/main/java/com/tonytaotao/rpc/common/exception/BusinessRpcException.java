package com.tonytaotao.rpc.common.exception;

public class BusinessRpcException extends AbstractRpcException {

    private static final long serialVersionUID = -812786666451764184L;

    public BusinessRpcException() {
    }

    public BusinessRpcException(String message) {
        super(message);
    }

    public BusinessRpcException(Throwable cause) {
        super(cause);
    }

    public BusinessRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
