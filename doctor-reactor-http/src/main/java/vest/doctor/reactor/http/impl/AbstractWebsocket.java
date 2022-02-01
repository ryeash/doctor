package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import vest.doctor.ProviderRegistry;
import vest.doctor.reactor.http.RunOn;
import vest.doctor.reactor.http.Websocket;
import vest.doctor.reactor.http.WebsocketSession;

import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class AbstractWebsocket implements Websocket {
    protected static final Logger log = LoggerFactory.getLogger(AbstractWebsocket.class);

    protected final Scheduler scheduler;

    protected AbstractWebsocket(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    protected AbstractWebsocket(ProviderRegistry providerRegistry) {
        this(providerRegistry, RunOn.DEFAULT_SCHEDULER);
    }

    protected AbstractWebsocket(ProviderRegistry providerRegistry, String executorName) {
        this(providerRegistry.getInstance(Scheduler.class, executorName));
    }

    @Override
    public Publisher<Void> apply(WebsocketInbound websocketInbound, WebsocketOutbound websocketOutbound) {
        return onConnect(new WebsocketSessionImpl(UUID.randomUUID(), websocketInbound, websocketOutbound, new ConcurrentSkipListMap<>()));
    }

    protected Publisher<Void> onConnect(WebsocketSession websocketSession) {
        websocketSession.inbound()
                .receiveFrames()
                .doOnNext(frame -> {
                    try {
                        onMessage(websocketSession, frame);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                })
                .subscribeOn(scheduler)
                .doOnError(throwable -> {
                    log.error("unexpected error processing inbound websocket frame; session will be terminated", throwable);
                    websocketSession.outbound().sendClose(1011, throwable.getMessage());
                })
                .subscribe();

        return websocketSession.outbound().sendObject(Flux.from(websocketSession));
    }

    protected void onMessage(WebsocketSession session, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame text) {
            onTextMessage(session, text);
        } else if (frame instanceof BinaryWebSocketFrame binary) {
            onBinaryMessage(session, binary);
        } else if (frame instanceof ContinuationWebSocketFrame cont) {
            onContinuationMessage(session, cont);
        } else if (frame instanceof PingWebSocketFrame ping) {
            onPingMessage(session, ping);
        } else if (frame instanceof PongWebSocketFrame pong) {
            onPongMessage(session, pong);
        } else if (frame instanceof CloseWebSocketFrame close) {
            onCloseMessage(session, close);
        } else {
            throw new IllegalArgumentException("unsupported websocket frame: " + frame);
        }
    }

    /**
     * Called when a text frame is received from a connection.
     *
     * @param session the session that received the message
     * @param frame   the text frame
     */
    protected abstract void onTextMessage(WebsocketSession session, TextWebSocketFrame frame) throws Exception;

    /**
     * Called when a binary frame is received from a connection.
     *
     * @param session the session that received the message
     * @param frame   the binary frame
     */
    protected abstract void onBinaryMessage(WebsocketSession session, BinaryWebSocketFrame frame) throws Exception;

    /**
     * Called when a continuation frame is received from a connection. The default
     * implementation throws an {@link UnsupportedOperationException}
     *
     * @param session the session that received the message
     * @param frame   the continuation frame
     */
    protected void onContinuationMessage(WebsocketSession session, ContinuationWebSocketFrame frame) throws Exception {
        throw new UnsupportedOperationException("fragmented messages are not supported");
    }

    /**
     * Called when a ping frame is received from a connection. The default implementation
     * responds with a pong frame using a copy of the received data.
     *
     * @param session the session that received the message
     * @param frame   the ping frame
     */
    protected void onPingMessage(WebsocketSession session, PingWebSocketFrame frame) {
        session.send(new PongWebSocketFrame(frame.content().copy()));
    }

    /**
     * Called when a pong frame is received from a connection. The default implementation
     * is a no-op.
     *
     * @param session the session that received the message
     * @param frame   the pong frame
     */
    protected void onPongMessage(WebsocketSession session, PongWebSocketFrame frame) {
        // no-op
    }

    /**
     * Called when a close frame is received from a connection. The default implementation
     * responds with a close frame and closes the connection.
     *
     * @param session the session that received the message
     * @param frame   the close frame
     */
    protected void onCloseMessage(WebsocketSession session, CloseWebSocketFrame frame) {
        session.sendClose(frame.statusCode(), frame.reasonText());
    }
}
