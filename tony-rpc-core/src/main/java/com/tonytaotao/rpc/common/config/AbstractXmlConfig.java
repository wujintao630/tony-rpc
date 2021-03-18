package com.tonytaotao.rpc.common.config;

import java.io.Serializable;


public class AbstractXmlConfig implements Serializable {

    private static final long serialVersionUID = -6047235443917923115L;

    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
