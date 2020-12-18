package vest.doctor.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
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
import io.netty.handler.codec.http.DefaultHttpResponse;
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
import vest.doctor.netty.impl.CompositeExceptionHandler;
import vest.doctor.netty.impl.ServerRequest;
import vest.doctor.netty.impl.StreamingRequestBody;
import vest.doctor.netty.impl.WebsocketHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class HttpServer extends SimpleChannelInboundHandler<HttpObject> implements AutoCloseable, Thread.UncaughtExceptionHandler {
    private static final AttributeKey<StreamingRequestBody> CONTEXT_BODY = AttributeKey.newInstance("doctor.netty.contextBody");
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    private final NettyConfiguration config;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final List<Channel> serverChannels;
    private final SslContext sslContext;
    private final Handler handler;
    private final Map<String, Supplier<Websocket>> websockets;
    private final ExceptionHandler exceptionHandler;

    public HttpServer(NettyConfiguration config, Handler handler) {
        this(config, handler, new CompositeExceptionHandler());
    }

    public HttpServer(NettyConfiguration config, Handler handler, ExceptionHandler exceptionHandler) {
        super();
        this.config = config;
        this.sslContext = config.getSslContext();
        this.handler = handler;
        this.websockets = new HashMap<>();
        this.exceptionHandler = exceptionHandler;
        this.bossGroup = new NioEventLoopGroup(config.getTcpManagementThreads(), new DefaultThreadFactory(config.getTcpThreadPrefix(), false));
        this.workerGroup = new NioEventLoopGroup(config.getWorkerThreads(), new DefaultThreadFactory(config.getWorkerThreadPrefix(), true));
        ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new NettyChannelInit(this));

        this.serverChannels = new LinkedList<>();
        for (InetSocketAddress inetSocketAddress : config.getBindAddresses()) {
            log.info("netty http server binding to {}", inetSocketAddress);
            serverChannels.add(b.bind(inetSocketAddress).channel());
        }
    }

    public void addWebsocket(String uri, Websocket websocket) {
        addWebsocket(uri, () -> websocket);
    }

    public void addWebsocket(String uri, Supplier<Websocket> websocket) {
        if (websockets.containsKey(uri)) {
            throw new IllegalArgumentException("there is already a websocket registered for " + uri);
        }
        websockets.put(uri, websocket);
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
        // upgrade to websocket connection, if requested
        String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
        if (upgradeHeader != null) {
            handleWebsocketUpgrade(ctx, request, upgradeHeader);
            return;
        }

        // handle as a normal HTTP request
        StreamingRequestBody body = new StreamingRequestBody(config.getMaxContentLength());
        if (!expectingContent(request)) {
            // end it since there is no body expected
            body.append(LastHttpContent.EMPTY_LAST_CONTENT);
        }
        ctx.channel().attr(CONTEXT_BODY).set(body);

        ServerRequest req = new ServerRequest(request, ctx, workerGroup, body);

        try {
            handler.handle(req)
                    .exceptionally(error -> handleError(req, error))
                    .thenAccept(response -> writeResponse(response.request(), response));
        } catch (Throwable t) {
            Response errorResponse = handleError(req, t);
            writeResponse(req, errorResponse);
        }
    }

    private Response handleError(Request request, Throwable error) {
        try {
            return exceptionHandler.handle(request, error);
        } catch (Throwable fatal) {
            log.error("error mapping exception", fatal);
            log.error("original exception", error);
            request.channelContext().close();
            return request.createResponse().status(500);
        }
    }

    private void handleBodyData(ChannelHandlerContext ctx, HttpContent content) {
        StreamingRequestBody streamingRequestBody = ctx.channel().attr(CONTEXT_BODY).get();
        if (streamingRequestBody != null) {
            streamingRequestBody.append(content.retainedDuplicate());
        }
    }

    private void writeResponse(Request request, Response response) {
        try {
            ChannelHandlerContext ctx = request.channelContext();
            HttpResponse nettyResponse = new DefaultHttpResponse(HTTP_1_1, response.status(), response.headers());
            ctx.write(nettyResponse);
            ChannelFuture f = response.body().writeTo(ctx);
            ctx.channel().attr(CONTEXT_BODY).set(null);
            ctx.flush();
            if (!(HttpUtil.isKeepAlive(request.unwrap()) && HttpUtil.isKeepAlive(nettyResponse))) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable t) {
            log.error("error writing response", t);
            request.channelContext().close();
        }
    }

    private void handleWebsocketUpgrade(ChannelHandlerContext ctx, HttpRequest request, String upgradeHeader) {
        if (HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
            Websocket ws = Optional.ofNullable(websockets.get(request.uri()))
                    .map(Supplier::get)
                    .orElse(null);
            if (ws != null) {
                // add the websocket handler to the end of the processing pipeline
                ctx.pipeline().removeLast();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpServer:\n");
        sb.append("Handler: ").append(handler).append("\n");
        if (!websockets.isEmpty()) {
            sb.append("Websockets:\n");
            for (Map.Entry<String, Supplier<Websocket>> entry : websockets.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" -> ").append(entry.getValue().get()).append("\n");
            }
        }
        sb.append("ExceptionHandler: ").append(exceptionHandler);
        return sb.toString();
    }

    private static boolean expectingContent(HttpRequest request) {
        return request.headers().getInt(HttpHeaderNames.CONTENT_LENGTH, 0) > 0
                || request.headers().contains(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true);
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
            // the ordering of these handlers is very sensitive
            p.addLast(new HttpServerCodec(config.getMaxInitialLineLength(),
                    config.getMaxHeaderSize(),
                    config.getMaxChunkSize(),
                    config.isValidateHeaders(),
                    config.getInitialBufferSize()));
            p.addLast(new HttpContentDecompressor());
            p.addLast(new HttpContentCompressor(6, 15, 8, 812));
            p.addLast(new ChunkedWriteHandler());
            p.addLast(server);
        }
    }
}