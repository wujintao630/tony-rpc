package com.tonytaotao.rpc.core.response.future;

public enum FutureStateEnum {
    /** the task is doing **/
    NEW(0),
    /** the task is done **/
    DONE(1),
    /** ths task is cancelled **/
    CANCELLED(2);

    private int state;

    FutureStateEnum(int state) {
        this.state = state;
    }
}
