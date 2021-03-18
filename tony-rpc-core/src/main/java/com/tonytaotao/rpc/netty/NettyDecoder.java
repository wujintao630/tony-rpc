package com.tonytaotao.rpc.netty;

import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.codec.Codec;
import com.tonytaotao.rpc.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger logger = LoggerFactory.getLogger(NettyDecoder.class);

    private int maxFrameLength;
    private Codec codec;
    private URL url;

    public NettyDecoder(Codec codec, URL url, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        this.codec = codec;
        this.url = url;
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in == null) {
            return null;
        }

        if (in.readableBytes() <= Constants.HEADER_SIZE) {
            return null;
        }

        in.markReaderIndex();

        short magicType = in.readShort();
        if (magicType != Constants.NETTY_MAGIC_TYPE) {
            in.resetReaderIndex();
            throw new FrameworkRpcException("RpcDecoder transport header not support, type: " + magicType);
        }

        byte messageType = in.readByte();
        long requestId = in.readLong();
        int dataLength = in.readInt();

        // FIXME 如果dataLength过大，可能导致问题
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return null;
        }

        if (maxFrameLength > 0 && dataLength > maxFrameLength) {
            logger.warn("NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
                    dataLength, maxFrameLength, ctx.channel().remoteAddress(), ctx.channel().localAddress());
            Exception e = new FrameworkRpcException("NettyDecoder transport data content length over of limit, size: "
                    + dataLength + " > " + maxFrameLength);

            if (messageType == Constants.FLAG_REQUEST) {
                Response response = buildExceptionResponse(requestId, e);
                ctx.write(response);
                throw e;
            } else {
                throw e;
            }
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);

        try {
            return codec.decode(url, messageType, data);
        } catch (Exception e) {
            if (messageType == Constants.FLAG_REQUEST) {
                Response response = buildExceptionResponse(requestId, e);
                ctx.write(response);
                return null;
            } else {
                Response response = buildExceptionResponse(requestId, e);
                return response;
            }
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }
}
