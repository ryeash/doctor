package vest.doctor.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;
import vest.doctor.ProviderRegistry;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Sharable
public class HttpServer extends SimpleChannelInboundHandler<HttpObject> implements AutoCloseable, Thread.UncaughtExceptionHandler {
    private static final AttributeKey<StreamingBody> contextBody = AttributeKey.newInstance("doctor.netty.contextBody");
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    private final NettyConfiguration config;
    private final EventLoopGroup bossGroup;
    private final List<Channel> serverChannels;
    private final SslContext sslContext;
    private final Router router;
    private final ExecutorService workers;

    public HttpServer(ProviderRegistry providerRegistry, Router router) {
        super();
        this.config = new NettyConfiguration(providerRegistry.configuration());
        this.sslContext = config.getSslContext();
        this.router = router;
        this.bossGroup = new NioEventLoopGroup(config.getTcpManagementThreads(), new DefaultThreadFactory(config.getTcpThreadPrefix(), false));
        ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                .childHandler(new NettyChannelInit(this));

        this.serverChannels = new LinkedList<>();
        for (InetSocketAddress inetSocketAddress : config.getListenAddresses()) {
            log.info("netty http server binding to {}", inetSocketAddress);
            serverChannels.add(b.bind(inetSocketAddress).channel());
        }

        CustomThreadFactory threadFactory = new CustomThreadFactory(false, config.getWorkerThreadPrefix(), this, null);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(config.getWorkerThreads(), config.getWorkerThreads(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
        tpe.prestartAllCoreThreads();
        tpe.allowCoreThreadTimeOut(false);
        tpe.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        this.workers = Executors.unconfigurableExecutorService(tpe);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error processing request: {}", ctx, cause);
        ctx.close();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("uncaught exception from worker thread", e);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject object) {
        try {
            if (object.decoderResult().isFailure()) {
                log.error("error reading HTTP request: {}", object.decoderResult());
                ctx.close();
                return;
            }
            if (object instanceof HttpRequest) {
                startRequestContext(ctx, (HttpRequest) object);
            } else if (object instanceof HttpContent) {
                handleBodyData(ctx, (HttpContent) object);
            }
        } catch (Throwable t) {
            log.error("error doing http stuff", t);
            throw t;
        }
    }

    private void startRequestContext(ChannelHandlerContext ctx, HttpRequest request) {
        String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
        if (upgradeHeader != null) {
            handleWebsocketUpgrade(ctx, request, upgradeHeader);
            return;
        }

        // handle as a normal HTTP request
        StreamingBody body = new StreamingBody(config.getMaxContentLength());
        if (request.headers().getInt(HttpHeaderNames.CONTENT_LENGTH, 0) == 0
                && !request.headers().contains(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true)) {
            // end it since there is no body expected
            body.append(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
        }

        RequestContext requestContext = new RequestContext(ctx, request, body);
        ctx.channel().attr(contextBody).set(body);

        requestContext.future().whenComplete(this::handleCompletedContext);

        workers.submit(() -> {
            try {
                boolean handled = router.accept(requestContext);
                if (!handled) {
                    requestContext.response(404, "");
                    requestContext.complete();
                }
            } catch (Throwable throwable) {
                handleError(requestContext, throwable);
                requestContext.complete();
            }
        });
    }

    private void handleBodyData(ChannelHandlerContext ctx, HttpContent content) {
        HttpContent dup = content.retainedDuplicate();
        StreamingBody streamingBody = ctx.channel().attr(contextBody).get();
        workers.submit(() -> streamingBody.append(dup));
    }

    private void handleCompletedContext(RequestContext requestContext, Throwable error) {
        if (error != null) {
            handleError(requestContext, error);
        }
        writeResponse(requestContext);
    }

    private void writeResponse(RequestContext requestContext) {
        try {
            ChannelHandlerContext ctx = requestContext.channelContext();
            HttpResponse nettyResponse = requestContext.buildResponse();
            HttpContent httpContent = requestContext.buildResponseBody();
            ctx.write(nettyResponse);
            ctx.write(httpContent);
            ctx.channel().attr(contextBody).set(null);
            ChannelFuture f = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            if (!HttpUtil.isKeepAlive(requestContext.request()) || !HttpUtil.isKeepAlive(nettyResponse)) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable t) {
            log.error("error writing response", t);
        }
    }

    private void handleError(RequestContext requestContext, Throwable throwable) {
        log.warn("error during route execution; request uri: {}", requestContext.requestUri(), throwable);

        requestContext.response(HttpResponseStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
        requestContext.responseHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        requestContext.responseHeader(HttpHeaderNames.CONNECTION, "close");

        Throwable temp = throwable;
        for (int i = 0; i < 4 && temp != null; i++) {
            if (temp instanceof HttpException) {
                requestContext.response(((HttpException) temp).status(), temp.getMessage());
                return;
            }
            temp = temp.getCause();
        }

        if (throwable instanceof IllegalArgumentException) {
            requestContext.response(HttpResponseStatus.BAD_REQUEST, throwable.getMessage());
        } else if (throwable instanceof InvocationTargetException && throwable.getCause() != null) {
            requestContext.response(HttpResponseStatus.INTERNAL_SERVER_ERROR, throwable.getCause().getMessage());
        }
    }

    private void handleWebsocketUpgrade(ChannelHandlerContext ctx, HttpRequest request, String upgradeHeader) {
        if (HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
            Websocket ws = router.getWebsocket(request.uri());
            if (ws != null) {
                // add the websocket handler to the pipeline
                ctx.pipeline().addLast(new WebsocketHandler(ws));
                ws.handshake(ctx, request, request.uri());
                return;
            } else {
                log.error("no websocket handler has been registered for path {}", request.uri());
            }
        } else {
            log.error("upgrading connection to '{}' is not supported", upgradeHeader);
        }
        DefaultFullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(nettyResponse)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                log.trace("ignored", e);
            }
        }
        bossGroup.shutdownGracefully();
    }

    private final class NettyChannelInit extends ChannelInitializer<SocketChannel> {

        private final HttpServer server;

        private NettyChannelInit(HttpServer server) {
            this.server = server;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
                p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast(new HttpServerCodec(
                    config.getMaxInitialLineLength(),
                    config.getMaxHeaderSize(),
                    config.getMaxChunkSize(),
                    config.isValidateHeaders(),
                    config.getInitialBufferSize()));
            p.addLast(new HttpContentCompressor(6, 15, 8, 812));
            p.addLast(new HttpContentDecompressor());
//        p.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
            p.addLast(new ChunkedWriteHandler());
            p.addLast(new HttpContentCompressor());
            p.addLast(server);
        }
    }
}