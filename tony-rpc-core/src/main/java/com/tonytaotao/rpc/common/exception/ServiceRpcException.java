package com.tonytaotao.rpc.common.exception;

public class ServiceRpcException extends AbstractRpcException {

    private static final long serialVersionUID = -6585936752307757973L;

    public ServiceRpcException() {
    }

    public ServiceRpcException(Throwable cause) {
        super(cause);
    }

    public ServiceRpcException(String message) {
        super(message);
    }

    public ServiceRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
