package vest.doctor.jersey;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;
import vest.doctor.ProviderRegistry;
import vest.doctor.netty.common.HttpServerConfiguration;
import vest.doctor.netty.common.Websocket;
import vest.doctor.netty.common.WebsocketRouter;
import vest.doctor.runtime.LoggingUncaughtExceptionHandler;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Sharable
final class JerseyChannelAdapter extends ChannelInboundHandlerAdapter {
    public static final String NETTY_REQUEST = "doctor.netty.request";
    public static final String NETTY_SERVLET_REQUEST = "doctor.netty.servlet.request";
    public static final String RESOURCE_CONFIG = "doctor.jersey.resourceConfig";

    private static final Logger log = LoggerFactory.getLogger(JerseyChannelAdapter.class);
    private final HttpServerConfiguration httpConfig;
    private final ApplicationHandler applicationHandler;
    private final ResourceConfig resourceConfig;
    private final Provider<SecurityContext> securityContextProvider;
    private final WebsocketRouter wsRouter = new WebsocketRouter();
    private final ExecutorService workerGroup;

    JerseyChannelAdapter(HttpServerConfiguration httpConfig,
                         Container container,
                         ProviderRegistry providerRegistry) {
        this.httpConfig = httpConfig;
        this.applicationHandler = container.getApplicationHandler();
        this.resourceConfig = container.getConfiguration();
        this.securityContextProvider = providerRegistry.getProviderOpt(SecurityContext.class)
                .map(p -> (Provider<SecurityContext>) p)
                .orElse(DefaultSecurityContext.PROVIDER);
        providerRegistry.getProviders(Websocket.class)
                .forEach(w -> wsRouter.addWebsocket(w::get));
        this.workerGroup = Executors.newFixedThreadPool(httpConfig.getWorkerThreads(), new CustomThreadFactory(true, httpConfig.getWorkerThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest req) {
            String upgradeHeader = req.headers().get(HttpHeaderNames.UPGRADE);
            if (upgradeHeader != null) {
                wsRouter.handleWebsocketUpgrade(ctx, req, upgradeHeader);
                return;
            }

            InputStream entityStream = new ByteBufInputStream(req.content(), true);

            ContainerRequest requestContext = createContainerRequest(ctx, req);
            requestContext.setProperty(NETTY_REQUEST, req);
            requestContext.setProperty(NETTY_SERVLET_REQUEST, new NettyHttpServletRequest(ctx, req));
            requestContext.setProperty(RESOURCE_CONFIG, resourceConfig);
            requestContext.setWriter(new NettyResponseWriter(ctx, req, entityStream));
            requestContext.setEntityStream(entityStream);
            for (String name : req.headers().names()) {
                requestContext.headers(name, req.headers().getAll(name));
            }
            CompletableFuture.completedFuture(requestContext)
                    .thenAcceptAsync(applicationHandler::handle, workerGroup)
                    .whenComplete((v, error) -> {
                        if (error != null) {
                            log.error("error processing request", error);
                            ctx.close();
                        }
                    });
        } else {
            log.error("unhandled object type: {}", msg);
            ctx.close();
        }
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
}
