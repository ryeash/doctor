package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Sinks;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import vest.doctor.reactor.http.WebsocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WebsocketSessionImpl implements WebsocketSession {
    private final UUID id;
    private final WebsocketInbound inbound;
    private final WebsocketOutbound outbound;
    private final Map<String, Object> attributes;

    private final Sinks.Many<WebSocketFrame> outputFrames;

    public WebsocketSessionImpl(UUID id,
                                WebsocketInbound inbound,
                                WebsocketOutbound outbound,
                                Map<String, Object> attributes) {
        this.id = id;
        this.inbound = inbound;
        this.outbound = outbound;
        this.attributes = attributes;

        this.outputFrames = Sinks.many()
                .unicast()
                .onBackpressureBuffer();
    }

    @Override
    public void attribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public <T> T attribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public <T> T attributeOrElse(String attribute, T orElse) {
        return (T) attributes.getOrDefault(attribute, orElse);
    }

    @Override
    public Set<String> attributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    @Override
    public void send(WebSocketFrame frame) {
        outputFrames.tryEmitNext(frame).orThrow();
        if (frame instanceof CloseWebSocketFrame) {
            outputFrames.tryEmitComplete().orThrow();
        }
    }

    public UUID id() {
        return id;
    }

    public WebsocketInbound inbound() {
        return inbound;
    }

    public WebsocketOutbound outbound() {
        return outbound;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WebsocketSessionImpl) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.inbound, that.inbound) &&
                Objects.equals(this.outbound, that.outbound) &&
                Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, inbound, outbound, attributes);
    }

    @Override
    public String toString() {
        return "WebsocketSessionImpl[" +
                "id=" + id + ", " +
                "inbound=" + inbound + ", " +
                "outbound=" + outbound + ", " +
                "attributes=" + attributes + ']';
    }

    @Override
    public void subscribe(Subscriber<? super WebSocketFrame> s) {
        outputFrames.asFlux().subscribe(s);
    }
}
