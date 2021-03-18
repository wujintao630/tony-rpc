package com.tonytaotao.rpc.core.response;

import java.util.Map;

public interface Response {

    Long getRequestId();

    Exception getException();

    Object getResult();

    Map<String, String> getAttachments();

    String getAttachment(String key);

    String getAttachment(String key, String defaultValue);

    void setAttachment(String key, String value);
}
