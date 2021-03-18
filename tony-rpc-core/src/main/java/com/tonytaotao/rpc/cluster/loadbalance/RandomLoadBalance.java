package com.tonytaotao.rpc.cluster.loadbalance;

import com.tonytaotao.rpc.cluster.LoadBalance;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.reference.Reference;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalance<T> implements LoadBalance<T> {
    private volatile List<Reference<T>> references;

    @Override
    public void setReferences(List<Reference<T>> references) {
        this.references = references;
    }

    @Override
    public Reference select(Request request) {
        int idx = (int) (ThreadLocalRandom.current().nextDouble() * references.size());
        return references.get(idx);
    }
}
