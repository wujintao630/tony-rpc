package com.tonytaotao.rpc.core;

import com.tonytaotao.rpc.common.URL;

public interface Node {

    void init();

    void destroy();

    boolean isAvailable();

    String desc();

    URL getUrl();
}
