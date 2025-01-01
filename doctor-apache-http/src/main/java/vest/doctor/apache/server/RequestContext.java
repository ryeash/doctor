package vest.doctor.apache.server;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import vest.doctor.apache.server.impl.CollectingSubscriber;
import vest.doctor.apache.server.impl.Router;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface RequestContext {

    String traceId();

    HttpRequest request();

    EntityDetails entityDetails();

    HttpContext context();

    Flow.Publisher<byte[]> dataInput();

    void send(AsyncResponseBuilder response) throws HttpException, IOException;

    default void setAttribute(String id, Object obj) {
        context().setAttribute(id, obj);
    }

    @SuppressWarnings("unchecked")
    default <T> T getAttribute(String id) {
        return (T) context().getAttribute(id);
    }

    default String getPathParam(String name) {
        return getPathParam(name, null, Function.identity());
    }

    default <T> T getPathParam(String name, T defaultValue, Function<String, T> mapper) {
        Map<String, String> pathParams = getAttribute(Router.PATH_PARAMS);
        return Optional.ofNullable(pathParams)
                .map(m -> m.get(name))
                .map(mapper)
                .orElse(defaultValue);
    }

    default CompletableFuture<String> collectBodyToString() {
        return collectBody(Collectors.collectingAndThen(CollectingSubscriber.BYTE_ARRAY_COLLECTOR, b -> new String(b, StandardCharsets.UTF_8)));
    }

    default CompletableFuture<byte[]> collectBodyToArray() {
        return collectBody(CollectingSubscriber.BYTE_ARRAY_COLLECTOR);
    }

    default CompletableFuture<Void> ignoreBody() {
        return collectBody(CollectingSubscriber.IGNORING);
    }

    default <T> CompletableFuture<T> collectBody(Collector<byte[], ?, T> collector) {
        CompletableFuture<T> future = new CompletableFuture<>();
        dataInput().subscribe(new CollectingSubscriber<>(collector, future));
        return future;
    }
}
