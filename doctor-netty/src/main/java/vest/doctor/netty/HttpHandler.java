package vest.doctor.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

import static io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static Logger log = LoggerFactory.getLogger(HttpHandler.class);
    private final NettyConfiguration config;
    private final Router router;

    public HttpHandler(NettyConfiguration config, Router router) {
        super();
        this.config = config;
        this.router = router;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error processing request: {}", ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // verify we got a compliant http request
        if (request.decoderResult().isFailure()) {
            log.error("error reading HTTP request: {}", request.decoderResult());
            ctx.close();
            return;
        }

        RequestContext requestContext = new RequestContext(ctx, request);

        // check for websocket upgrade request
        String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
        if (upgradeHeader != null) {
            if (HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
                handleWebsocketUpgrade(ctx, request, requestContext);
            } else {
                // if a request wants to upgrade to something other than 'websocket', return an error
                requestContext.complete(new HttpException(HttpResponseStatus.BAD_REQUEST, "upgrading connection to '" + upgradeHeader + "' is not supported"));
            }
            return;
        }

        // handle as a normal HTTP request
        try {
            router.accept(requestContext);
        } catch (Throwable throwable) {
            handleError(requestContext, throwable);
            requestContext.complete();
        }

        requestContext.future().whenComplete((n, error) -> {
            if (error != null) {
                handleError(requestContext, error);
            }
            writeResponse(ctx, request, requestContext);
        });
    }

    private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext requestContext) {
        try {
            HttpResponse nettyResponse = requestContext.buildResponse();
            ChannelFuture f = ctx.writeAndFlush(nettyResponse)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            if (!HttpUtil.isKeepAlive(request) || !HttpUtil.isKeepAlive(nettyResponse)) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable t) {
            log.error("error writing response", t);
        }
    }

    private void handleError(RequestContext requestContext, Throwable throwable) {
        log.error("error during route execution; request uri: {}", requestContext.requestUri(), throwable);

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

    private void handleWebsocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext requestContext) {
        Websocket ws = router.getWebsocket(request.uri());
        if (ws == null) {
            requestContext.response(HttpResponseStatus.BAD_REQUEST, "no websocket handler has been registered for path " + request.uri());
            requestContext.complete();
        } else {
            // add the websocket handler to the pipeline
            ctx.pipeline().addLast(new WebsocketHandler(ws));
            ws.handshake(ctx, request, requestContext);
        }
    }
}