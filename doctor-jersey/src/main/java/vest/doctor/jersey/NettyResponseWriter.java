package vest.doctor.jersey;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import org.glassfish.jersey.netty.connector.internal.JerseyChunkedInput;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class NettyResponseWriter implements ContainerResponseWriter {
    private static final Logger log = LoggerFactory.getLogger(NettyResponseWriter.class.getName());
    private static final ChannelFutureListener FLUSH_FUTURE = future -> future.channel().flush();
    private final ChannelHandlerContext ctx;
    private final HttpRequest req;
    private final DoctorJerseyContainer container;
    private volatile ScheduledFuture<?> suspendTimeoutFuture;
    private volatile Runnable suspendTimeoutHandler;
    private boolean responseWritten = false;

    NettyResponseWriter(ChannelHandlerContext ctx, HttpRequest req, DoctorJerseyContainer container) {
        this.ctx = ctx;
        this.req = req;
        this.container = container;
    }

    @Override
    public synchronized OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) throws ContainerException {
        if (responseWritten) {
            log.trace("Response already written.");
            return null;
        } else {
            responseWritten = true;
            String reasonPhrase = responseContext.getStatusInfo().getReasonPhrase();
            int statusCode = responseContext.getStatus();
            HttpResponseStatus status = reasonPhrase == null
                    ? HttpResponseStatus.valueOf(statusCode)
                    : new HttpResponseStatus(statusCode, reasonPhrase);
            DefaultHttpResponse response = contentLength == 0L
                    ? new DefaultFullHttpResponse(req.protocolVersion(), status)
                    : new DefaultHttpResponse(req.protocolVersion(), status);

            for (Map.Entry<String, List<String>> e : responseContext.getStringHeaders().entrySet()) {
                response.headers().add(e.getKey(), e.getValue());
            }

            if (contentLength == -1L) {
                HttpUtil.setTransferEncodingChunked(response, true);
            } else {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            }

            if (HttpUtil.isKeepAlive(req)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ctx.writeAndFlush(response);
            if (req.method() != HttpMethod.HEAD && (contentLength > 0L || contentLength == -1L)) {
                JerseyChunkedInput jerseyChunkedInput = new JerseyChunkedInput(ctx.channel());
                if (HttpUtil.isTransferEncodingChunked(response)) {
                    ctx.writeAndFlush(new HttpChunkedInput(jerseyChunkedInput));
                } else {
                    ctx.write(new HttpChunkedInput(jerseyChunkedInput)).addListener(FLUSH_FUTURE);
                }
                return jerseyChunkedInput;
            } else {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                return null;
            }
        }
    }

    @Override
    public boolean suspend(long timeOut, TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
        suspendTimeoutHandler = () -> timeoutHandler.onTimeout(this);
        if (timeOut > 0L) {
            suspendTimeoutFuture = container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);
        }
        return true;
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
        if (suspendTimeoutFuture != null) {
            suspendTimeoutFuture.cancel(true);
        }
        if (timeOut > 0L) {
            suspendTimeoutFuture = container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);
        }
    }

    @Override
    public void commit() {
        ctx.flush();
    }

    @Override
    public void failure(Throwable error) {
        log.error("error in response writer", error);
        ctx.writeAndFlush(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR))
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public boolean enableResponseBuffering() {
        return true;
    }
}
