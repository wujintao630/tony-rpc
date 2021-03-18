package com.tonytaotao.rpc.common.exception;

public class FrameworkRpcException extends AbstractRpcException {

    private static final long serialVersionUID = -3361435023080270457L;

    public FrameworkRpcException() {
    }

    public FrameworkRpcException(String message) {
        super(message);
    }

    public FrameworkRpcException(Throwable cause) {
        super(cause);
    }

    public FrameworkRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
