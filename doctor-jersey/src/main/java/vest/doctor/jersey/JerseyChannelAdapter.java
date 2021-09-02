package vest.doctor.jersey;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.netty.connector.internal.NettyInputStream;
import org.glassfish.jersey.netty.httpserver.NettySecurityContext;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;

final class JerseyChannelAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(JerseyChannelAdapter.class);
    private final HttpServerConfiguration httpConfig;
    private final NettyInputStream nettyInputStream = new NettyInputStream();
    private final NettyHttpContainer container;
    private final ResourceConfig resourceConfig;

    public JerseyChannelAdapter(HttpServerConfiguration httpConfig, NettyHttpContainer container, ResourceConfig resourceConfig) {
        this.httpConfig = httpConfig;
        this.container = container;
        this.resourceConfig = resourceConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest req) {
            if (HttpUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            nettyInputStream.clear();
            ContainerRequest requestContext = createContainerRequest(ctx, req);
            requestContext.setWriter(new NettyResponseWriter(ctx, req, container));
            long contentLength = req.headers().contains(HttpHeaderNames.CONTENT_LENGTH) ? HttpUtil.getContentLength(req) : -1L;
            if (contentLength >= httpConfig.getMaxContentLength()) {
                requestContext.abortWith(Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build());
            } else {
                String contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
                boolean isJson = contentType != null && contentType.toLowerCase().contains("application/json");
                if (!isJson && contentLength != -1L || HttpUtil.isTransferEncodingChunked(req) || isJson && contentLength >= 2L) {
                    requestContext.setEntityStream(nettyInputStream);
                }
            }

            for (String name : req.headers().names()) {
                requestContext.headers(name, req.headers().getAll(name));
            }

            container.handle(requestContext);
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error in channel context", cause);
        ctx.close();
    }

    private ContainerRequest createContainerRequest(ChannelHandlerContext ctx, HttpRequest req) {
        String hostHeader = req.headers().get(HttpHeaderNames.HOST);
        String uri = req.uri();
        URI requestUri;
        if (uri.startsWith("http")) {
            requestUri = URI.create(uri);
        } else if (hostHeader != null) {
            String scheme = httpConfig.getSslContext() != null ? "https" : "http";
            requestUri = URI.create(scheme + "://" + hostHeader + req.uri());
        } else {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            String scheme = httpConfig.getSslContext() != null ? "https" : "http";
            String host = socketAddress.getHostName();
            int port = socketAddress.getPort();
            requestUri = URI.create(scheme + "://" + host + ":" + port + req.uri());
        }
        URI baseUri = URI.create(requestUri.getScheme() + "://" + requestUri.getHost() + ":" + requestUri.getPort() + "/");

        return new ContainerRequest(baseUri, requestUri, req.method().name(), new NettySecurityContext(ctx), new MapPropertiesDelegate(), resourceConfig);
    }
}
