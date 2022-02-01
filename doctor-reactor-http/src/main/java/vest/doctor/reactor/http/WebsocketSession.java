package vest.doctor.reactor.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.UUID;

public interface WebsocketSession extends Publisher<WebSocketFrame> {

    UUID id();

    WebsocketInbound inbound();

    WebsocketOutbound outbound();

    void attribute(String name, Object value);

    <T> T attribute(String name);

    <T> T attributeOrElse(String attribute, T orElse);

    void send(WebSocketFrame frame);

    default void sendText(String text) {
        send(new TextWebSocketFrame(text));
    }

    default void sendBinary(ByteBuf buffer) {
        send(new BinaryWebSocketFrame(buffer));
    }

    default void sendClose(int status, String message) {
        send(new CloseWebSocketFrame(status, message));
    }
}
