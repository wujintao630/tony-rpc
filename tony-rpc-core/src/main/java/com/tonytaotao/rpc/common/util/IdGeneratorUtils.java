package com.tonytaotao.rpc.common.util;

import java.util.concurrent.atomic.AtomicLong;

public class IdGeneratorUtils {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    public static long getRequestId() {
        return idGenerator.getAndIncrement();
    }
}
