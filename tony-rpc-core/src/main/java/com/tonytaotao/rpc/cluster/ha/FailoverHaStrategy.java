package com.tonytaotao.rpc.cluster.ha;


import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.common.exception.BusinessRpcException;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.core.reference.Reference;
import com.tonytaotao.rpc.cluster.HaStrategy;
import com.tonytaotao.rpc.cluster.LoadBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverHaStrategy<T> implements HaStrategy<T> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Response call(Request request, LoadBalance loadBalance) {
        Reference<T> reference = loadBalance.select(request);
        URL refUrl = reference.getUrl();
        int tryCount = refUrl.getIntParameterByEnum(UrlParamEnum.retries);
        if(tryCount<0){
            tryCount = 0;
        }
        for (int i = 0; i <= tryCount; i++) {
            reference = loadBalance.select(request);
            try {
                return reference.call(request);
            } catch (RuntimeException e) {
                // 对于业务异常，直接抛出
                if (e instanceof BusinessRpcException) {
                    throw e;
                } else if (i >= tryCount) {
                    throw e;
                }
                logger.warn(String.format("FailoverHaStrategy Call false for request:%s error=%s", request, e.getMessage()));
            }
        }
        throw new FrameworkRpcException("FailoverHaStrategy.call should not come here!");
    }
}
