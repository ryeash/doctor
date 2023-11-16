package vest.doctor.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;
import vest.doctor.http.server.impl.HttpException;
import vest.doctor.http.server.impl.HttpServerChannelInitializer;
import vest.doctor.http.server.impl.LoggingUncaughtExceptionHandler;
import vest.doctor.http.server.impl.RequestContextImpl;
import vest.doctor.http.server.impl.ServerRequest;
import vest.doctor.http.server.impl.ServerResponse;
import vest.doctor.http.server.impl.WebsocketRouter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class Server extends SimpleChannelInboundHandler<FullHttpRequest> implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final List<Channel> serverChannels;
    private final HttpServerConfiguration config;
    private final Handler handler;
    private final WebsocketRouter wsRouter;

    public Server(HttpServerConfiguration config, Handler handler, WebsocketRouter websocketRouter) {
        this.config = config;
        if (config.getBindAddresses() == null || config.getBindAddresses().isEmpty()) {
            throw new IllegalArgumentException("must set a bind address for the server to start");
        }
        this.handler = handler;
        ServerBootstrap bootstrap = new ServerBootstrap();
        this.bossGroup = new NioEventLoopGroup(config.getTcpManagementThreads(), new CustomThreadFactory(false, config.getTcpThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));
        this.workerGroup = new NioEventLoopGroup(config.getWorkerThreads(), new CustomThreadFactory(true, config.getWorkerThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));
        bootstrap.group(bossGroup);
        bootstrap.channelFactory(NioServerSocketChannel::new)
                .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new HttpServerChannelInitializer(this, config));

        if (config.getServerBootstrapCustomizers() != null) {
            for (ServerBootstrapCustomizer customizer : config.getServerBootstrapCustomizers()) {
                customizer.customize(bootstrap);
            }
        }

        this.serverChannels = config.getBindAddresses()
                .stream()
                .map(bootstrap::bind)
                .map(ChannelFuture::syncUninterruptibly)
                .map(ChannelFuture::channel)
                .collect(Collectors.toList());

        this.wsRouter = websocketRouter;
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            doQuietly(serverChannel::close);
        }
        doQuietly(bossGroup::shutdownGracefully);
        if (workerGroup != null) {
            doQuietly(workerGroup::shutdownGracefully);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error processing request: {}", ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest object) {
        try {
            if (object.decoderResult().isFailure()) {
                log.error("error reading HTTP request: {}", object.decoderResult());
                ctx.close();
                return;
            }
            handleRequest(ctx, object);
        } catch (Throwable t) {
            log.error("error handling http object", t);
            throw t;
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
        if (upgradeHeader != null) {
            wsRouter.handleWebsocketUpgrade(ctx, request, upgradeHeader);
            return;
        }

        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        request.retain(1);
        Request req = new ServerRequest(request);
        Response response = new ServerResponse(req);
        RequestContext requestContext = new RequestContextImpl(req, response, ctx);
        try {
            CompletableFuture.completedFuture(requestContext)
                    .thenComposeAsync(c -> {
                        try {
                            return handler.handle(c);
                        } catch (Exception e) {
                            throw new HttpException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
                        }
                    })
                    .whenComplete((r, error) -> {
                        if (error != null) {
                            writeResponse(ctx, handleError(requestContext, error));
                        } else {
                            writeResponse(ctx, r);
                        }
                    });
        } catch (Throwable t) {
            writeResponse(ctx, handleError(requestContext, t));
        }
    }

    private RequestContext handleError(RequestContext ctx, Throwable error) {
        try {
            return config.getExceptionHandler().handle(ctx, error);
        } catch (Throwable fatal) {
            log.error("error mapping exception", fatal);
            log.error("original exception", error);
            ctx.channelContext().close();
            ctx.response().status(500).body(ResponseBody.empty());
            return ctx;
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, RequestContext requestContext) {
        Response response = requestContext.response();
        Request request = response.request();
        try {
            HttpResponse nettyResponse = new DefaultHttpResponse(HTTP_1_1, response.status(), response.headers());
            ctx.write(nettyResponse);
            ChannelFuture f = response.body().writeTo(ctx);
            if (!(HttpUtil.isKeepAlive(request.unwrap()) && HttpUtil.isKeepAlive(nettyResponse))) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable t) {
            log.error("error writing response", t);
            ctx.close();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpServer:\n");
        sb.append("Handler: ").append(handler).append("\n");
        if (wsRouter.size() > 0) {
            sb.append("Websockets:\n");
            wsRouter.eachRoute((spec, ws) ->
                    sb.append("  ").append(spec.getPath()).append(" -> ").append(ws).append("\n"));
        }
        sb.append("ExceptionHandler: ").append(config.getExceptionHandler());
        return sb.toString();
    }

    private static void doQuietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // ignored
        }
    }
}
