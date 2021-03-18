package com.tonytaotao.rpc.serializer;


import com.tonytaotao.rpc.core.extension.SPI;
import com.tonytaotao.rpc.core.extension.Scope;

import java.io.IOException;

@SPI(value = "protostuff", scope = Scope.SINGLETON)
public interface Serializer {

    byte[] serialize(Object msg) throws IOException;

    <T> T deserialize(byte[] data, Class<T> type) throws IOException;
}
