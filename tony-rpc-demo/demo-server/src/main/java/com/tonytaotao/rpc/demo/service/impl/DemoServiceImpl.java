package com.tonytaotao.rpc.demo.service.impl;

import com.tonytaotao.rpc.demo.api.DemoService;

public class DemoServiceImpl implements DemoService {

    @Override
    public String hello() {
        return "hello, tonyrpc!";
    }
}
