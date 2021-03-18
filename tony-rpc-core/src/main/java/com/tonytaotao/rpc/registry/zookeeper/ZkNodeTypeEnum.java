package com.tonytaotao.rpc.registry.zookeeper;

public enum ZkNodeTypeEnum {

    SERVER("providers"),
    CLIENT("consumers");

    private String value;

    ZkNodeTypeEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
