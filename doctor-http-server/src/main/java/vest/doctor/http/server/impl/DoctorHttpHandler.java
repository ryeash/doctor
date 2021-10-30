package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.http.server.DoctorHttpServerConfiguration;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.netty.common.Websocket;
import vest.doctor.netty.common.WebsocketRouter;
import vest.doctor.workflow.Workflow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class DoctorHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final AttributeKey<HttpContentSource> CONTEXT_BODY = AttributeKey.newInstance("doctor.netty.contextBody");
    private static final AttributeKey<AtomicInteger> BODY_SIZE = AttributeKey.newInstance("doctor.netty.bodySize");

    private static final Logger log = LoggerFactory.getLogger(DoctorHttpHandler.class);
    private final DoctorHttpServerConfiguration config;
    private final Handler handler;
    private final ExceptionHandler exceptionHandler;
    private final WebsocketRouter wsRouter = new WebsocketRouter();

    public DoctorHttpHandler(DoctorHttpServerConfiguration config, Handler handler, ExceptionHandler exceptionHandler) {
        super();
        this.config = config;
        this.handler = handler;
        this.exceptionHandler = exceptionHandler;
    }

    public void addWebsocket(Supplier<Websocket> websocket) {
        wsRouter.addWebsocket(websocket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error processing request: {}", ctx, cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject object) {
        try {
            if (object.decoderResult().isFailure()) {
                log.error("error reading HTTP request: {}", object.decoderResult());
                ctx.close();
                return;
            }

            if (object instanceof HttpRequest request) {
                String upgradeHeader = request.headers().get(HttpHeaderNames.UPGRADE);
                if (upgradeHeader != null) {
                    wsRouter.handleWebsocketUpgrade(ctx, request, upgradeHeader);
                    return;
                }

                if (HttpUtil.is100ContinueExpected(request)) {
                    ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
                }

                HttpContentSource source = new HttpContentSource();
                Workflow<HttpContent, HttpContent> bodyData = Workflow.from(source)
                        .defaultExecutorService(ctx.executor())
//                        .takeWhile(c -> !(c instanceof LastHttpContent), true)
                        ;
                StreamingRequestBody body = new StreamingRequestBody(ctx, bodyData);
                ctx.channel().attr(CONTEXT_BODY).set(source);
                ctx.channel().attr(BODY_SIZE).set(new AtomicInteger(0));

                ServerRequest req = new ServerRequest(request, ctx, ctx.executor(), body);
                Workflow<?, Response> handle;
                try {
                    handle = handler.handle(req);
                } catch (Throwable t) {
                    handle = Workflow.<Response>error(t)
                            .defaultExecutorService(ctx.executor());
                }
                // ensure the body flow is subscribed
                if (!req.body().used()) {
                    req.body().ignored().subscribe();
                }
                handle.recover(error -> handleError(req, error))
                        .observe(response -> writeResponse(response.request(), response))
                        .subscribe();
            }

            if (object instanceof HttpContent content) {
                HttpContentSource bodyFlow = ctx.channel().attr(CONTEXT_BODY).get();
                AtomicInteger size = ctx.channel().attr(BODY_SIZE).get();
                if (size.addAndGet(content.content().readableBytes()) > config.getMaxContentLength()) {
                    throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, "");
                }
                if (bodyFlow != null) {
                    HttpContent httpContent = content.retainedDuplicate();
                    bodyFlow.onNext(httpContent);
                }
            }
        } catch (Throwable t) {
            log.error("error doing http stuff", t);
            throw t;
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
        sb.append("ExceptionHandler: ").append(exceptionHandler);
        return sb.toString();
    }
}
