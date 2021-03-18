package com.tonytaotao.rpc.core.response.future;

public interface ResponseFuture<T> {

    T get() throws InterruptedException;

    boolean isCancelled();

    boolean isDone();

    boolean isSuccess();

    void setResult(T result);

    void setFailure(Throwable err);

    boolean isTimeout();

}
