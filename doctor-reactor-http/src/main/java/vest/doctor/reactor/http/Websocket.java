package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.List;
import java.util.function.BiFunction;

public interface Websocket extends BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    List<String> path();
}
