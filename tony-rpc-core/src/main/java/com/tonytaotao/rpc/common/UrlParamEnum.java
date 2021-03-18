package com.tonytaotao.rpc.common;


public enum UrlParamEnum {

    application("application", ""),
    version("version", "1.0.0"),
    group("group", "default_rpc"),

    protocol("protocol", Constants.FRAMEWORK_NAME),
    path("path", ""),
    host("host", ""),
    port("port", 0),
    check("check", true),
    retries("retries", 0),
    requestTimeout("timeout", 500),
    connectTimeout("connectTimeout", 1000),

    loadBalance("loadbalance", "random"),
    haStrategy("haStrategy", "failfast"),
    side("side", ""),
    timestamp("timestamp", 0),

    /**netty**/
    minWorkerThread("minWorkerThread", 20),
    maxWorkerThread("maxWorkerThread", 200),
    maxContentLength("maxContentLength", 1<<24),
    bufferSize("buffer_size", 1024*16),

    /**proxy**/
    proxyType("proxy", "jdk"),
    /**ServiceCommon**/
    registryProtocol("reg_protocol", "local"),
    registryAddress("reg_address", "localhost"),
    registrySessionTimeout("reg_session_timeout", 60*1000),
    registryConnectTimeout("reg_connect_timeout", 5000),
    /** serialize **/
    serialization("serialization", "protostuff"),
    /** codec **/
    codec("codec", Constants.FRAMEWORK_NAME);

    private String name;
    private String defaultValue;
    private int intValue;
    private boolean boolValue;

    UrlParamEnum(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    UrlParamEnum(String name, int intValue) {
        this.name = name;
        this.defaultValue = String.valueOf(intValue);
        this.intValue = intValue;
    }

    UrlParamEnum(String name, boolean boolValue) {
        this.name = name;
        this.defaultValue = String.valueOf(boolValue);
        this.boolValue = boolValue;
    }

    public String getName() {
        return name;
    }

    public int getIntValue() {
        return intValue;
    }

    public boolean isBoolValue() {
        return boolValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
