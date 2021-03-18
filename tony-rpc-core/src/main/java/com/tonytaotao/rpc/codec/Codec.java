package com.tonytaotao.rpc.codec;


import com.tonytaotao.rpc.common.Constants;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.extension.SPI;

import java.io.IOException;

@SPI(value = Constants.FRAMEWORK_NAME)
public interface Codec {

    byte[] encode(URL url, Object message) throws IOException;

    Object decode(URL url, byte messageType, byte[] data) throws IOException;
}
