package vest.doctor.reactor.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.Set;
import java.util.UUID;

/**
 * A websocket session encapsulating the input and output channel streams, additionally providing
 * simplified helpers for send frames to the client.
 */
public interface WebsocketSession extends Publisher<WebSocketFrame> {

    /**
     * An immutable ID to use for tracking this session.
     */
    UUID id();

    /**
     * The {@link WebsocketInbound} for receiving frames from the client.
     */
    WebsocketInbound inbound();

    /**
     * The {@link WebsocketOutbound} for sending frames to the client.
     */
    WebsocketOutbound outbound();

    /**
     * Set an attribute on this session that will persist until the session terminated.
     *
     * @param name  the name of the attribute
     * @param value the value ofo the attribute
     */
    void attribute(String name, Object value);

    /**
     * Get an attribute value from this session.
     *
     * @param name the name of the attribute
     * @return the attribute value
     */
    <T> T attribute(String name);

    /**
     * Get an attribute value or a fallback value if it is not present.
     *
     * @param attribute the name of the attribute
     * @param orElse    the fallback value
     * @return the attribute value, or fallback
     */
    <T> T attributeOrElse(String attribute, T orElse);

    /**
     * Get the names of all attributes attached to this context.
     */
    Set<String> attributeNames();

    /**
     * Send a frame to the client.
     */
    void send(WebSocketFrame frame);

    /**
     * Send a text frame to the client.
     *
     * @param text the text to send
     */
    default void sendText(String text) {
        send(new TextWebSocketFrame(text));
    }

    /**
     * Send a binary frame to the client.
     *
     * @param buffer the data to send
     */
    default void sendBinary(ByteBuf buffer) {
        send(new BinaryWebSocketFrame(buffer));
    }

    /**
     * Send the close frame to the client.
     *
     * @param status  the close status
     * @param message the close message
     */
    default void sendClose(int status, String message) {
        send(new CloseWebSocketFrame(status, message));
    }
}
