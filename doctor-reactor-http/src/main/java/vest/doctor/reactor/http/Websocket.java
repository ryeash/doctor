package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A websocket endpoint registered in the HTTP server via
 * {@link reactor.netty.http.server.HttpServerRoutes#ws(String, BiFunction)}.
 */
public interface Websocket extends BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    /**
     * The paths that the websocket will be registered under.
     */
    List<String> path();
}
