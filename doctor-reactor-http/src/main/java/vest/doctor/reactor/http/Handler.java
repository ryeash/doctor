package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.List;
import java.util.function.BiFunction;

/**
 * A handler for HTTP requests. Implementations must conform to handlers registered via
 * {@link reactor.netty.http.server.HttpServer#handle(BiFunction)}.
 */
public interface Handler extends BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

    /**
     * The methods this handler will match against.
     */
    List<String> method();

    /**
     * The paths this handler will match against.
     * Path matching is based on reactor.netty.http.server.HttpPredicate.UriPathTemplate.
     */
    List<String> path();
}
