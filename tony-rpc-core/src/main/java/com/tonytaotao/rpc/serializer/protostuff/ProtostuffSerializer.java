package com.tonytaotao.rpc.serializer.protostuff;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tonytaotao.rpc.serializer.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ProtostuffSerializer implements Serializer {

    private static final LoadingCache<Class<?>, Schema<?>> schemas = CacheBuilder.newBuilder()
            .build(new CacheLoader<Class<?>, Schema<?>>() {
                @Override
                public Schema<?> load(Class<?> cls) throws Exception {

                    return RuntimeSchema.createFrom(cls);
                }
            });

    @Override
    public byte[] serialize(Object msg) throws IOException {
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema schema = getSchema(msg.getClass());
            byte[] arr = ProtostuffIOUtil.toByteArray(msg, schema, buffer);
            return arr;
        } finally {
            buffer.clear();
        }
    }

    @Override
    public <T> T deserialize(byte[] buf, Class<T> type) throws IOException {
        Schema<T> schema = getSchema(type);
        T msg = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(buf, msg, schema);
        return (T) msg;
    }

    private static Schema getSchema(Class<?> cls) throws IOException {
        try {
            return schemas.get(cls);
        } catch (ExecutionException e) {
            throw new IOException("create protostuff schema error", e);
        }
    }
}
