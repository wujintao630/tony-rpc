package com.tonytaotao.rpc.netty.server;

import com.tonytaotao.rpc.codec.Codec;
import com.tonytaotao.rpc.common.URL;
import com.tonytaotao.rpc.common.UrlParamEnum;
import com.tonytaotao.rpc.core.extension.ExtensionLoader;
import com.tonytaotao.rpc.core.request.DefaultRequest;
import com.tonytaotao.rpc.core.response.DefaultResponse;
import com.tonytaotao.rpc.common.exception.FrameworkRpcException;
import com.tonytaotao.rpc.core.message.DefaultMessageHandler;
import com.tonytaotao.rpc.core.RpcContext;
import com.tonytaotao.rpc.netty.ChannelStateEnum;
import com.tonytaotao.rpc.netty.NettyDecoder;
import com.tonytaotao.rpc.netty.NettyEncoder;
import com.tonytaotao.rpc.common.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultNettyServer implements NettyServer {

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap serverBootstrap = new ServerBootstrap();

    private ThreadPoolExecutor threadPoolExecutor;    //业务处理线程池
    private DefaultMessageHandler messageHandler;

    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    private URL url;
    private Codec codec;

    private volatile ChannelStateEnum state = ChannelStateEnum.NEW;

    private volatile boolean initializing = false;

    public DefaultNettyServer(URL url, DefaultMessageHandler messageHandler){
        this.url = url;
        this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getStrParameterByEnum(UrlParamEnum.codec));
        this.localAddress = new InetSocketAddress(url.getPort());
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = new ThreadPoolExecutor(url.getIntParameterByEnum(UrlParamEnum.minWorkerThread),
                url.getIntParameterByEnum(UrlParamEnum.maxWorkerThread),
                120, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new DefaultThreadFactory(String.format("%s-%s", Constants.FRAMEWORK_NAME, "biz")));
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
        if(initializing) {
            log.warn("NettyServer ServerChannel is initializing: url=" + url);
            return true;
        }
        initializing = true;

        if (state.isAvailable()) {
            log.warn("NettyServer ServerChannel has initialized: url=" + url);
            return true;
        }
        // 最大响应包限制
        final int maxContentLength = url.getIntParameterByEnum(UrlParamEnum.maxContentLength);

        this.serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_RCVBUF, url.getIntParameterByEnum(UrlParamEnum.bufferSize))
                .childOption(ChannelOption.SO_SNDBUF, url.getIntParameterByEnum(UrlParamEnum.bufferSize))
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws IOException {

                        ch.pipeline().addLast(new NettyDecoder(codec, url, maxContentLength, Constants.HEADER_SIZE, 4), //
                                new NettyEncoder(codec, url), //
                                new NettyServerHandler());
                    }
                });

        try {
            ChannelFuture channelFuture = this.serverBootstrap.bind(this.localAddress).sync();

            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {

                    if(f.isSuccess()){
                        log.info("Rpc Server bind port:{} success", url.getPort());
                    } else {
                        log.error("Rpc Server bind port:{} failure", url.getPort());
                    }
                }
            });
        } catch (InterruptedException e) {
            log.error(String.format("NettyServer bind to address:%s failure", this.localAddress), e);
            throw new FrameworkRpcException(String.format("NettyClient connect to address:%s failure", this.localAddress), e);
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
    public void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {

        if (state.isClosed()) {
            log.info("NettyServer close fail: already close, url={}", url.getUri());
            return;
        }

        try {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
            this.threadPoolExecutor.shutdown();

            state = ChannelStateEnum.CLOSED;
        } catch (Exception e) {
            log.error("NettyServer close Error: url=" + url.getUri(), e);
        }
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<DefaultRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext context, DefaultRequest request) throws Exception {

            log.info("Rpc server receive request id:{}", request.getRequestId());
            //处理请求
            processRpcRequest(context, request);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("NettyServerHandler exceptionCaught: remote=" + ctx.channel().remoteAddress()
                    + " local=" + ctx.channel().localAddress(), cause);
            ctx.channel().close();
        }
    }

    /**处理客户端请求**/
    private void processRpcRequest(final ChannelHandlerContext context, final DefaultRequest request) {
        final long processStartTime = System.currentTimeMillis();
        try {
            this.threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RpcContext.init(request);
                        processRpcRequest(context, request, processStartTime);
                    } finally {
                        RpcContext.destroy();
                    }

                }
            });
        } catch (RejectedExecutionException e) {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setException(new FrameworkRpcException("process thread pool is full, reject"));
            response.setProcessTime(System.currentTimeMillis() - processStartTime);
            context.channel().write(response);
        }

    }

    private void processRpcRequest(ChannelHandlerContext context, DefaultRequest request, long processStartTime) {

        DefaultResponse response = (DefaultResponse) this.messageHandler.handle(request);
        response.setProcessTime(System.currentTimeMillis() - processStartTime);
        if(request.getType()!=Constants.REQUEST_ONEWAY){    //非单向调用
            context.writeAndFlush(response);
        }
        log.info("Rpc server process request:{} end...", request.getRequestId());
    }
}
