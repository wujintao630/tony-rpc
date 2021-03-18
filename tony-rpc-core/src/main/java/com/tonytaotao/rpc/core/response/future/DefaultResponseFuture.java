package com.tonytaotao.rpc.core.response.future;

import com.tonytaotao.rpc.common.exception.FrameworkRpcException;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

public class DefaultResponseFuture<T> implements ResponseFuture<T> {


    private volatile FutureStateEnum state = FutureStateEnum.NEW; //状态

    private final long createTime = System.currentTimeMillis();//处理开始时间

    private volatile T result;
    private volatile Throwable err;
    private CountDownLatch latch;

    private long timeoutInMillis;

    public DefaultResponseFuture(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
    }

    @Override
    public boolean isCancelled() {
        return this.state == FutureStateEnum.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return this.state == FutureStateEnum.DONE;
    }

    @Override
    public boolean isTimeout() {
        return createTime + timeoutInMillis > System.currentTimeMillis();
    }


    @Override
    public T get() throws InterruptedException {
        if(!this.isDone()) {
            boolean wait = this.prepareForWait();
            if(wait) {
                this.latch.await();
            }
        }
        return returnResult();
    }

    @Override
    public boolean isSuccess() {
        return isDone() && err==null;
    }

    @Override
    public void setResult(T result) {
        synchronized(this) {
            if(!this.isDone()) {
                this.result = result;
                this.state = FutureStateEnum.DONE;
                if(this.latch != null) {
                    this.latch.countDown();
                }
            }
        }
    }

    @Override
    public void setFailure(Throwable throwable) {
        if(!(throwable instanceof IOException) && !(throwable instanceof SecurityException)) {
            throwable = new IOException(throwable);
        }

        synchronized(this) {
            if(!this.isDone()) {
                this.err = throwable;
                this.state = FutureStateEnum.DONE;
                if(this.latch != null) {
                    this.latch.countDown();
                }
            }
        }
    }

    private T returnResult() throws CancellationException {
        if(this.err != null) {
            if(this.state == FutureStateEnum.CANCELLED) {
                throw new CancellationException();
            } else {
                throw new FrameworkRpcException(this.err);
            }
        } else {
            return this.result;
        }
    }

    private boolean prepareForWait() {
        synchronized(this) {
            if(this.isDone()) {
                return false;
            } else {
                if(this.latch == null) {
                    this.latch = new CountDownLatch(1);
                }
                return true;
            }
        }
    }
}
