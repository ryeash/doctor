package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract class defining a websocket endpoint.
 */
public abstract class Websocket {

    public static final AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER = AttributeKey.valueOf("vest.websocket.handshaker");
    public static final AttributeKey<String> WS_PATH = AttributeKey.valueOf("vest.websocket.path");
    public static final WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory("/*", null, false);

    /**
     * Called when the HttpServer accepts a new websocket connection. Defaults to a no-op.
     *
     * @param ctx  The client connection context
     * @param path The path that the websocket client connected to
     */
    public void connect(ChannelHandlerContext ctx, String path) {
        // default to no-op
    }

    /**
     * Called when a message is received from the client.
     *
     * @param ctx   The connection context that received the message.
     * @param frame The websocket message frame
     */
    public abstract void onMessage(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception;

    /**
     * Get the text data from the websocket frame.
     *
     * @param frame the frame to get the data from
     * @return a string
     */
    protected String getText(WebSocketFrame frame) {
        return frame.content().toString(StandardCharsets.UTF_8);
    }

    /**
     * Get the binary data from the websocket frame.
     *
     * @param frame the frame to get the data from
     * @return a byte array
     */
    protected byte[] getBinary(WebSocketFrame frame) {
        ByteBuf content = frame.content();
        byte[] arr = new byte[content.readableBytes()];
        content.readBytes(arr, 0, arr.length);
        return arr;
    }

    /**
     * Send a websocket text frame.
     *
     * @param ctx  The context to send the message to
     * @param text The message to send
     */
    protected CompletableFuture<Void> sendText(ChannelHandlerContext ctx, String text) {
        return send(ctx, new TextWebSocketFrame(text));
    }

    /**
     * Send a websocket text frame.
     *
     * @param ctx     The context to send the message to
     * @param payload The message to send
     */
    protected CompletableFuture<Void> sendText(ChannelHandlerContext ctx, ByteBuf payload) {
        return send(ctx, new TextWebSocketFrame(payload));
    }

    /**
     * Send a websocket binary frame.
     *
     * @param ctx     The context to send the message to
     * @param payload The payload to send
     */
    protected CompletableFuture<Void> sendBinary(ChannelHandlerContext ctx, byte[] payload) {
        return sendBinary(ctx, Unpooled.wrappedBuffer(payload));
    }

    /**
     * Send a websocket binary frame.
     *
     * @param ctx     The context to send the message to
     * @param payload The payload to send
     */
    protected CompletableFuture<Void> sendBinary(ChannelHandlerContext ctx, ByteBuf payload) {
        return send(ctx, new BinaryWebSocketFrame(payload));
    }

    /**
     * Send a websocket frame.
     *
     * @param ctx   The context to send the message to
     * @param frame The frame to send
     */
    protected CompletableFuture<Void> send(ChannelHandlerContext ctx, WebSocketFrame frame) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.channel().eventLoop().execute(() -> ctx.writeAndFlush(frame).addListener(f -> future.complete(null)));
        return future;
    }

    /**
     * Close the channel. By default this will send a status 1000 with the message "server closed".
     *
     * @param ctx The websocket channel context to close
     */
    protected void close(ChannelHandlerContext ctx) {
        close(ctx, 1000, "server closed");
    }

    /**
     * Close the channel with the given close status and message.
     *
     * @param ctx     The websocket channel context to close
     * @param status  The close status to send
     * @param message The message to send
     */
    protected void close(ChannelHandlerContext ctx, int status, String message) {
        if (ctx != null && ctx.channel() != null && ctx.channel().isOpen()) {
            WebSocketServerHandshaker handshaker = ctx.channel().attr(WS_HANDSHAKER).get();
            handshaker.close(ctx.channel(), new CloseWebSocketFrame(status, message));
        }
    }

    /**
     * Handshakes the websocket connection.
     *
     * @param ctx            The context to handshake
     * @param request        The http request that initiated the websocket handshake
     * @param requestContext The requestContext
     */
    public final void handshake(ChannelHandlerContext ctx, FullHttpRequest request, RequestContext requestContext) {
        WebSocketServerHandshaker handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ctx.channel().attr(WS_HANDSHAKER).set(handshaker);
            ctx.channel().attr(WS_PATH).set(request.uri());
            handshaker.handshake(ctx.channel(), request).addListener(future -> {
                if (future.isSuccess()) {
                    connect(ctx, requestContext.requestPath());
                }
            });
        }
    }
}