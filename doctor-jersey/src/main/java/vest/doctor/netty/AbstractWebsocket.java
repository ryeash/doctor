package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract implementation for a {@link Websocket}. Adds frame type routing and helper methods to simplify
 * use of websockets.
 */
public abstract class AbstractWebsocket implements Websocket {

    private static final AttributeKey<WebSocketServerHandshaker> WS_HANDSHAKER = AttributeKey.valueOf("doctor.websocket.handshaker");
    private static final AttributeKey<String> WS_PATH = AttributeKey.valueOf("doctor.websocket.path");
    private static final WebSocketServerHandshakerFactory HANDSHAKER_FACTORY = new WebSocketServerHandshakerFactory("/*", null, false);

    @Override
    public void connect(ChannelHandlerContext ctx, String path, Map<String, String> params) {
        // default to no-op
    }

    /**
     * Routes the websocket frame to the corresponding handler method.
     *
     * @param ctx   The connection context that received the message.
     * @param frame The websocket message frame
     * @see #onTextMessage(ChannelHandlerContext, TextWebSocketFrame)
     * @see #onBinaryMessage(ChannelHandlerContext, BinaryWebSocketFrame)
     */
    @Override
    public final void onMessage(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame text) {
            onTextMessage(ctx, text);
        } else if (frame instanceof BinaryWebSocketFrame binary) {
            onBinaryMessage(ctx, binary);
        } else if (frame instanceof ContinuationWebSocketFrame cont) {
            onContinuationMessage(ctx, cont);
        } else if (frame instanceof PingWebSocketFrame ping) {
            onPingMessage(ctx, ping);
        } else if (frame instanceof PongWebSocketFrame pong) {
            onPongMessage(ctx, pong);
        } else if (frame instanceof CloseWebSocketFrame close) {
            onCloseMessage(ctx, close);
        } else {
            throw new IllegalArgumentException("unsupported websocket frame: " + frame);
        }
    }

    /**
     * Called when a text frame is received from a connection.
     *
     * @param ctx   the connection that received the message
     * @param frame the text frame
     */
    protected abstract void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception;

    /**
     * Called when a binary frame is received from a connection.
     *
     * @param ctx   the connection that received the message
     * @param frame the binary frame
     */
    protected abstract void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) throws Exception;

    /**
     * Called when a continuation frame is received from a connection. The default
     * implementation throws an {@link UnsupportedOperationException}
     *
     * @param ctx   the connection that received the message
     * @param frame the continuation frame
     */
    protected void onContinuationMessage(ChannelHandlerContext ctx, ContinuationWebSocketFrame frame) throws Exception {
        throw new UnsupportedOperationException("fragmented messages are not supported");
    }

    /**
     * Called when a ping frame is received from a connection. The default implementation
     * responds with a pong frame using a copy of the received data.
     *
     * @param ctx   the connection that received the message
     * @param frame the ping frame
     */
    protected void onPingMessage(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        send(ctx, new PongWebSocketFrame(frame.content().copy()));
    }

    /**
     * Called when a pong frame is received from a connection. The default implementation
     * is a no-op.
     *
     * @param ctx   the connection that received the message
     * @param frame the pong frame
     */
    protected void onPongMessage(ChannelHandlerContext ctx, PongWebSocketFrame frame) {
        // no-op
    }

    /**
     * Called when a close frame is received from a connection. The default implementation
     * responds with a close frame and closes the connection.
     *
     * @param ctx   the connection that received the message
     * @param frame the close frame
     */
    protected void onCloseMessage(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        close(ctx, frame.statusCode(), frame.reasonText());
    }

    /**
     * Helper method. Copy all readable bytes from the buffer into a byte array.
     *
     * @param content the buffer to read
     * @return a byte array of the readable data from the buffer
     */
    protected byte[] toByteArray(ByteBuf content) {
        byte[] arr = new byte[content.readableBytes()];
        content.readBytes(arr, 0, arr.length);
        return arr;
    }

    /**
     * Helper method. Convert the buffer to an input stream.
     *
     * @param content the buffer to read
     * @return an {@link InputStream} over the data in the buffer
     */
    protected InputStream toInputStream(ByteBuf content) {
        return new ByteBufInputStream(content, content.readableBytes(), true);
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
     * Send a websocket frame to the client.
     *
     * @param ctx   The channel to send the message on
     * @param frame The frame to send
     * @return a future representing the future completed send of data
     */
    protected CompletableFuture<Void> send(ChannelHandlerContext ctx, WebSocketFrame frame) {
        CompletableFuture<ChannelFuture> future = new CompletableFuture<>();
        ctx.channel()
                .eventLoop()
                .submit(() -> ctx.writeAndFlush(frame))
                .addListener(new CompletableFutureListener<>(future));
        return future.thenApply(f -> null);
    }

    /**
     * Send a close frame and perform the close handshake with the remote.
     * The status code and message used in the close frame are left to the
     * implementation to decide.
     *
     * @param ctx The websocket channel context to close
     */
    protected void close(ChannelHandlerContext ctx) {
        close(ctx, 1000, "server closed");
    }

    @Override
    public void close(ChannelHandlerContext ctx, int status, String message) {
        if (ctx != null && ctx.channel() != null && ctx.channel().isOpen()) {
            WebSocketServerHandshaker handshaker = ctx.channel().attr(WS_HANDSHAKER).get();
            if (handshaker != null) {
                handshaker.close(ctx.channel(), new CloseWebSocketFrame(status, message));
            } else {
                ctx.channel().close();
            }
        }
    }

    @Override
    public final void handshake(ChannelHandlerContext ctx, HttpRequest request, String path, Map<String, String> params) {
        WebSocketServerHandshaker handshaker = HANDSHAKER_FACTORY.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ctx.channel().attr(WS_HANDSHAKER).set(handshaker);
            ctx.channel().attr(WS_PATH).set(request.uri());
            handshaker.handshake(ctx.channel(), request).addListener(future -> {
                if (future.isSuccess()) {
                    connect(ctx, path, params);
                }
            });
        }
    }
}
