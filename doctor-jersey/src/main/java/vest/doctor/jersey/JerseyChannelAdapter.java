package vest.doctor.jersey;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.ProviderRegistry;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vest.doctor.jersey.DoctorChannelInitializer.JERSEY_ADAPTER_NAME;

@Sharable
final class JerseyChannelAdapter extends ChannelInboundHandlerAdapter {
    public static final AttributeKey<QueuedBufInputStream> INPUT_STREAM = AttributeKey.newInstance("doctor.netty.isstream");
    public static final String NETTY_REQUEST = "doctor.netty.request";
    public static final String NETTY_SERVLET_REQUEST = "doctor.netty.servlet.request";
    public static final String RESOURCE_CONFIG = "doctor.jersey.resourceConfig";
    private static final Logger log = LoggerFactory.getLogger(JerseyChannelAdapter.class);
    private final HttpServerConfiguration httpConfig;
    private final ApplicationHandler applicationHandler;
    private final EventLoopGroup workerGroup;
    private final ResourceConfig resourceConfig;
    private final Provider<SecurityContext> securityContextProvider;
    private final Map<String, Provider<Websocket>> websockets;

    public JerseyChannelAdapter(HttpServerConfiguration httpConfig,
                                Container container,
                                EventLoopGroup workerGroup,
                                ProviderRegistry providerRegistry) {
        this.httpConfig = httpConfig;
        this.applicationHandler = container.getApplicationHandler();
        this.workerGroup = workerGroup;
        this.resourceConfig = container.getConfiguration();
        this.securityContextProvider = providerRegistry.getProviderOpt(SecurityContext.class)
                .map(p -> (Provider<SecurityContext>) p)
                .orElse(DefaultSecurityContext.PROVIDER);
        this.websockets = providerRegistry.getProviders(Websocket.class)
                .collect(Collectors.toUnmodifiableMap(p -> p.get().path(), Function.identity()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpObject)) {
            log.error("unhandled object type: " + msg);
            ctx.close();
        }

        if (msg instanceof HttpRequest req) {
            if (HttpUtil.getContentLength(req, -1L) >= httpConfig.getMaxContentLength()) {
                sendErrorAndClose(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
                return;
            }

            if (HttpUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            String upgradeHeader = req.headers().get(HttpHeaderNames.UPGRADE);
            if (upgradeHeader != null) {
                handleWebsocketUpgrade(ctx, req, upgradeHeader);
                return;
            }

            QueuedBufInputStream stream = ctx.channel().attr(INPUT_STREAM).get();
            if (stream == null) {
                stream = new QueuedBufInputStream();
                ctx.channel().attr(INPUT_STREAM).set(stream);
            } else {
                stream.reset();
            }

            ContainerRequest requestContext = createContainerRequest(ctx, req);
            requestContext.setProperty(NETTY_REQUEST, req);
            requestContext.setProperty(NETTY_SERVLET_REQUEST, new NettyHttpServletRequest(ctx, req));
            requestContext.setProperty(RESOURCE_CONFIG, resourceConfig);
            requestContext.setWriter(new NettyResponseWriter(ctx, req));
            requestContext.setEntityStream(stream);
            for (String name : req.headers().names()) {
                requestContext.headers(name, req.headers().getAll(name));
            }

            workerGroup.submit(() -> applicationHandler.handle(requestContext));
        }

        if (msg instanceof HttpContent httpContent) {
            QueuedBufInputStream is = ctx.channel().attr(INPUT_STREAM).get();
            if (is != null) {
                is.append(httpContent);
                if (is.size() > httpConfig.getMaxContentLength()) {
                    sendErrorAndClose(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        QueuedBufInputStream buf = ctx.channel().attr(INPUT_STREAM).get();
        if (buf != null) {
            buf.reset();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error in channel context", cause);
        ctx.close();
    }

    private ContainerRequest createContainerRequest(ChannelHandlerContext ctx, HttpRequest req) {
        String hostHeader = req.headers().get(HttpHeaderNames.HOST);
        String uri = req.uri();
        URI requestUri;
        String scheme = httpConfig.getSslContext() != null ? "https" : "http";
        if (uri.startsWith("http")) {
            requestUri = URI.create(uri);
        } else if (hostHeader != null) {
            requestUri = URI.create(scheme + "://" + hostHeader + req.uri());
        } else {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            String host = socketAddress.getHostName();
            int port = socketAddress.getPort();
            requestUri = URI.create(scheme + "://" + host + ":" + port + req.uri());
        }
        URI baseUri = URI.create(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + "/");
        return new ContainerRequest(baseUri, requestUri, req.method().name(), securityContextProvider.get(), new MapPropertiesDelegate(), resourceConfig);
    }

    private void handleWebsocketUpgrade(ChannelHandlerContext ctx, HttpRequest request, String upgradeHeader) {
        if (HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
            Websocket ws = Optional.ofNullable(websockets.get(request.uri()))
                    .map(Provider::get)
                    .orElse(null);
            if (ws != null) {
                // add the websocket handler to the end of the processing pipeline
                ctx.pipeline().replace(JERSEY_ADAPTER_NAME, "websocketHandler", new WebsocketHandler(ws));
                ws.handshake(ctx, request, request.uri());
                return;
            } else {
                log.error("no websocket handler has been registered for path {}", request.uri());
            }
        } else {
            log.error("upgrading connection to '{}' is not supported", upgradeHeader);
        }
        sendErrorAndClose(ctx, HttpResponseStatus.BAD_REQUEST);
    }

    private static void sendErrorAndClose(ChannelHandlerContext ctx, HttpResponseStatus status) {
        DefaultFullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(nettyResponse)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
