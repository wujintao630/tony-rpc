package com.tonytaotao.rpc.netty;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.codec.Codec;
import com.tonytaotao.rpc.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(NettyEncoder.class);
    private Codec codec;
    private URL url;

    public NettyEncoder(Codec codec, URL url) {
        this.codec = codec;
        this.url = url;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

        long requestId = getRequestId(msg);
        byte[] data = null;

        if (msg instanceof Response) {
            try {
                data = codec.encode(url, msg);
            } catch (Exception e) {
                logger.error("RpcEncoder encode error, requestId=" + requestId, e);
                Response response = buildExceptionResponse(requestId, e);
                data = codec.encode(url, response);
            }
        } else {
            data = codec.encode(url, msg);
        }

        out.writeShort(Constants.NETTY_MAGIC_TYPE);
        out.writeByte(getType(msg));
        out.writeLong(requestId);
        out.writeInt(data.length);

        out.writeBytes(data);
    }

    private byte getType(Object message) {
        if (message instanceof Request) {
            return Constants.FLAG_REQUEST;
        } else if (message instanceof Response) {
            return Constants.FLAG_RESPONSE;
        } else {
            return Constants.FLAG_OTHER;
        }
    }

    private long getRequestId(Object message) {
        if (message instanceof Request) {
            return ((Request) message).getRequestId();
        } else if (message instanceof Response) {
            return ((Response) message).getRequestId();
        } else {
            return 0;
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }
}
