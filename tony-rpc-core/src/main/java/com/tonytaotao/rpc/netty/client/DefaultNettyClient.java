package com.tonytaotao.rpc.netty.client;

import com.tonytaotao.rpc.codec.Codec;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.core.request.Request;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.core.response.Response;
import com.tonytaotao.rpc.core.response.future.DefaultResponseFuture;
import com.tonytaotao.rpc.core.response.future.ResponseFuture;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.common.exception.TransportRpcException;
import com.tonytaotao.rpc.netty.ChannelStateEnum;
import com.tonytaotao.rpc.netty.ChannelWrapper;
import com.tonytaotao.rpc.netty.NettyDecoder;
import com.tonytaotao.rpc.netty.NettyEncoder;
import com.tonytaotao.rpc.common.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class DefaultNettyClient implements NettyClient {

    private EventLoopGroup group = new NioEventLoopGroup();
    private Bootstrap b = new Bootstrap();

    private final ConcurrentHashMap<Long, ResponseFuture> responseFutureMap = new ConcurrentHashMap<>(256);

    private ScheduledExecutorService scheduledExecutorService;
    private int timeout;

    private volatile boolean initializing;

    private volatile ChannelWrapper channelWrapper;

    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    private URL url;
    private Codec codec;

    private volatile ChannelStateEnum state = ChannelStateEnum.NEW;

    public DefaultNettyClient(URL url) {
        this.url = url;
        this.remoteAddress = new InetSocketAddress(url.getHost(), url.getPort());

        this.timeout = url.getIntParameterByEnum(UrlParamEnum.requestTimeout);

        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(5, new DefaultThreadFactory(String.format("%s-%s", Constants.FRAMEWORK_NAME, "future")));

        this.scheduledExecutorService.scheduleAtFixedRate(() -> { scanRpcFutureTable(); }, 0, 5000, TimeUnit.MILLISECONDS);

        this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getStrParameterByEnum(UrlParamEnum.codec));

        log.info("NettyClient init url:" + url.getHost() + "-" + url.getPath() + ", use codec:" + codec.getClass().getSimpleName());
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public synchronized boolean open() {

        if(initializing){
            log.warn("NettyClient is initializing: url=" + url);
            return true;
        }
        initializing = true;

        if(state.isAvailable()){
            log.warn("NettyClient has initialized: url=" + url);
            return true;
        }

        // 最大响应包限制
        final int maxContentLength = url.getIntParameterByEnum(UrlParamEnum.maxContentLength);

        b.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, url.getIntParameterByEnum(UrlParamEnum.bufferSize))
                .option(ChannelOption.SO_SNDBUF, url.getIntParameterByEnum(UrlParamEnum.bufferSize))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new NettyDecoder(codec, url, maxContentLength, Constants.HEADER_SIZE, 4),
                                new NettyEncoder(codec, url),
                                new NettyClientHandler());
                    }
                });

        try {
            ChannelFuture channelFuture = b.connect(this.remoteAddress).sync();
            this.channelWrapper = new ChannelWrapper(channelFuture);
        } catch (InterruptedException e) {
            log.error(String.format("NettyClient connect to address:%s failure", this.remoteAddress), e);
            throw new FrameworkRpcException(String.format("NettyClient connect to address:%s failure"), e);
        }

        state = ChannelStateEnum.AVAILABLE;
        return true;
    }

    @Override
    public boolean isAvailable() {
        return state.isAvailable();
    }

    @Override
    public boolean isClosed() {
        return state.isClosed();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Response invokeSync(final Request request) throws InterruptedException, TransportRpcException {
        Channel channel = getChannel();
        if (channel != null && channel.isActive()) {
            final ResponseFuture<Response> rpcFuture = new DefaultResponseFuture<>(timeout);
            this.responseFutureMap.put(request.getRequestId(), rpcFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        log.info("send success, request id:{}", request.getRequestId());

                    } else {
                        log.info("send failure, request id:{}", request.getRequestId());
                        responseFutureMap.remove(request.getRequestId());
                        rpcFuture.setFailure(future.cause());
                    }
                }
            });
            return rpcFuture.get();
        } else {
            throw new TransportRpcException("channel not active. request id:"+request.getRequestId());
        }
    }

    @Override
    public ResponseFuture invokeAsync(final Request request) throws InterruptedException, TransportRpcException {
        Channel channel = getChannel();
        if (channel != null && channel.isActive()) {

            final ResponseFuture<Response> rpcFuture = new DefaultResponseFuture<>(timeout);
            this.responseFutureMap.put(request.getRequestId(), rpcFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        log.info("send success, request id:{}", request.getRequestId());
                    }
                }
            });
            return rpcFuture;
        } else {
            throw new TransportRpcException("channel not active. request id:"+request.getRequestId());
        }
    }

    @Override
    public void invokeOneway(final Request request) throws InterruptedException, TransportRpcException {
        Channel channel = getChannel();
        if (channel != null && channel.isActive()) {
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        log.info("send success, request id:{}", request.getRequestId());
                    } else {
                        log.info("send failure, request id:{}", request.getRequestId());
                    }
                }
            });
        } else {
            throw new TransportRpcException("channel not active. request id:"+request.getRequestId());
        }
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {

        if(state.isClosed()){
            log.info("NettyClient close fail: already close, url={}", url.getUri());
            return;
        }

        try {
            this.scheduledExecutorService.shutdown();
            this.group.shutdownGracefully();

            state = ChannelStateEnum.CLOSED;
        } catch (Exception e) {
            log.error("NettyClient close Error: url=" + url.getUri(), e);
        }

    }

    private class NettyClientHandler extends ChannelInboundHandlerAdapter {
        private Logger logger = LoggerFactory.getLogger(getClass());

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            logger.info("client read msg:{}, ", msg);
            if(msg instanceof Response) {
                DefaultResponse response = (DefaultResponse) msg;

                ResponseFuture<Response> rpcFuture =responseFutureMap.get(response.getRequestId());
                if(rpcFuture!=null) {
                    responseFutureMap.remove(response.getRequestId());
                    rpcFuture.setResult(response);
                }

            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("client caught exception", cause);
            ctx.close();
        }
    }

    private Channel getChannel() throws InterruptedException {

        if (this.channelWrapper != null && this.channelWrapper.isActive()) {
            return this.channelWrapper.getChannel();
        }

        synchronized (this){
            // 发起异步连接操作
            ChannelFuture channelFuture = b.connect(this.remoteAddress).sync();
            this.channelWrapper = new ChannelWrapper(channelFuture);
        }

        return this.channelWrapper.getChannel();
    }

    /**定时清理超时Future**/
    private void scanRpcFutureTable() {

        long currentTime = System.currentTimeMillis();
        log.info("scan timeout RpcFuture, currentTime:{}", currentTime);

        final List<ResponseFuture> timeoutFutureList = new ArrayList<>();
        Iterator<Map.Entry<Long, ResponseFuture>> it = this.responseFutureMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ResponseFuture> next = it.next();
            ResponseFuture future = next.getValue();

            if (future.isTimeout()) {  //超时
                it.remove();
                timeoutFutureList.add(future);
            }
        }

        for (ResponseFuture future : timeoutFutureList) {
            //释放资源
        }
    }
}
