package org.glassfish.jersey.netty.httpserver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.netty.connector.internal.NettyInputStream;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ContainerUtils;
import vest.doctor.jaxrs.JAXRSConfiguration;
import vest.doctor.websocket.WebsocketRouter;

import java.net.URI;
import java.util.HashMap;

import static jakarta.ws.rs.core.Response.*;

final class JerseyServerHandler extends ChannelInboundHandlerAdapter {
    private static final long DEFAULT_MAX_REQUEST_ENTITY_BYTES = 8 * 1024 * 1024;

    private final JAXRSConfiguration config;
    private final URI baseUri;
    private final String applicationPath;
    private final NettyInputStream nettyInputStream = new NettyInputStream();
    private final NettyHttpContainer container;
    private final ResourceConfig resourceConfig;
    private final WebsocketRouter wsRouter;

    public JerseyServerHandler(JAXRSConfiguration config, URI baseUri, String applicationPath, NettyHttpContainer container, ResourceConfig resourceConfig, WebsocketRouter wsRouter) {
        this.config = config;
        this.baseUri = baseUri;
        this.container = container;
        this.resourceConfig = resourceConfig;
        this.applicationPath = applicationPath;
        this.wsRouter = wsRouter;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest req) {
            String upgradeHeader = req.headers().get(HttpHeaderNames.UPGRADE);
            if (upgradeHeader != null) {
                wsRouter.handleWebsocketUpgrade(ctx, req, upgradeHeader);
                return;
            }

            nettyInputStream.clear(); // clearing the content - possible leftover from previous request processing.
            ContainerRequest requestContext = createContainerRequest(ctx, req);

            requestContext.setWriter(new NettyResponseWriter(ctx, req, container));

            long contentLength = req.headers().contains(HttpHeaderNames.CONTENT_LENGTH)
                    ? HttpUtil.getContentLength(req)
                    : -1L;
            if (contentLength >= config.maxContentLength().orElse(DEFAULT_MAX_REQUEST_ENTITY_BYTES)) {
                requestContext.abortWith(status(Status.REQUEST_ENTITY_TOO_LARGE).build());
            } else {
                /*
                  Jackson JSON decoder tries to read a minimum of 2 bytes (4
                  for BOM). So, during an empty or 1-byte input, we'd want to
                  avoid reading the entity to safely handle this edge case by
                  eventually throwing a malformed JSON exception.
                 */
                String contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
                boolean isJson = contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON);
                //process entity streams only if there is an entity issued in the request (i.e., content-length >=0).
                //Otherwise, it's safe to discard during next processing
                if ((!isJson && contentLength != -1) || HttpUtil.isTransferEncodingChunked(req) || (isJson && contentLength >= 2)) {
                    requestContext.setEntityStream(nettyInputStream);
                }
            }

            // copying headers from netty request to jersey container request context.
            for (String name : req.headers().names()) {
                requestContext.headers(name, req.headers().getAll(name));
            }

            // must be like this, since there is a blocking read from Jersey
            container.getExecutorService().execute(() -> container.getApplicationHandler().handle(requestContext));
        }

        if (msg instanceof HttpContent httpContent) {
            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                nettyInputStream.publish(content);
            }
            if (msg instanceof LastHttpContent) {
                nettyInputStream.complete(null);
            }
        }
    }

    /**
     * Create Jersey {@link ContainerRequest} based on Netty {@link HttpRequest}.
     *
     * @param ctx Netty channel context.
     * @param req Netty Http request.
     * @return created Jersey Container Request.
     */
    private ContainerRequest createContainerRequest(ChannelHandlerContext ctx, HttpRequest req) {
        String s = req.uri().startsWith("/")
                ? req.uri().substring(1)
                : req.uri();
        String baseUriStr = baseUri.toString();
        String base = applicationPath == null || applicationPath.isEmpty()
                ? baseUriStr
                : baseUriStr.substring(0, baseUriStr.length() - applicationPath.length() - 1);
        URI requestUri = URI.create(base + ContainerUtils.encodeUnsafeCharacters(s));
        return new ContainerRequest(
                baseUri,
                requestUri,
                req.method().name(),
                getSecurityContext(ctx),
                new MapPropertiesDelegate(new HashMap<>()),
                resourceConfig);
    }

    private NettySecurityContext getSecurityContext(ChannelHandlerContext ctx) {
        return new NettySecurityContext(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
