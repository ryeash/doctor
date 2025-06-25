package vest.doctor.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.glassfish.jersey.netty.httpserver.NettyJersey;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

public final class WebsocketRouter {

    private final Map<PathSpec, Supplier<Websocket>> websockets = new ConcurrentSkipListMap<>();

    public void addWebsocket(String path, Supplier<Websocket> ws) {
        PathSpec pathSpec = new PathSpec(path, true);
        if (websockets.containsKey(pathSpec)) {
            throw new IllegalArgumentException("there is already a websocket registered for " + path);
        }
        websockets.put(pathSpec, ws);
    }

    public void handleWebsocketUpgrade(ChannelHandlerContext ctx, HttpRequest request, String upgradeHeader) {
        String errorMsg;
        if (HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
            QueryStringDecoder qsd = new QueryStringDecoder(request.uri());
            for (Map.Entry<PathSpec, Supplier<Websocket>> entry : websockets.entrySet()) {
                Map<String, String> params = entry.getKey().matchAndCollect(qsd.rawPath());
                if (params != null) {
                    Websocket ws = entry.getValue().get();
                    // add the websocket handler to the end of the processing pipeline
                    ctx.pipeline().replace(NettyJersey.JERSEY_HANDLER, "websocketHandler", new WebsocketHandler(ws));
                    ws.handshake(ctx, request, qsd.rawPath(), params);
                    return;
                }
            }
            errorMsg = "no websocket handler has been registered for path " + request.uri();
        } else {
            errorMsg = "upgrading connection to '" + upgradeHeader + "' is not supported";
        }
        DefaultFullHttpResponse err = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(errorMsg.getBytes(StandardCharsets.UTF_8)));
        err.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(err).addListener(ChannelFutureListener.CLOSE);
    }
}