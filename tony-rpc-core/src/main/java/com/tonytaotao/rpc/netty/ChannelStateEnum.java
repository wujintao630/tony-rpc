package com.tonytaotao.rpc.netty;

public enum ChannelStateEnum {

    /** 未初始化状态 **/
    NEW(0),
    /** 初始化完成 **/
    INIT(1),
    /** 存活可用状态 **/
    AVAILABLE(2),
    /** 不可用状态 **/
    UNAVAILABLE(3),
    /** 关闭状态 **/
    CLOSED(4);

    public final int value;

    ChannelStateEnum(int value) {
        this.value = value;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean isUnavailable() {
        return this == UNAVAILABLE;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }

    public boolean isInitialized() {
        return this == INIT;
    }

    public boolean isNew() {
        return this == NEW;
    }
}
