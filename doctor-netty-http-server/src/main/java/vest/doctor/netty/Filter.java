package vest.doctor.netty;

import vest.doctor.Prioritized;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface Filter extends Prioritized {

    CompletableFuture<Response> filter(Request request, CompletableFuture<Response> response);

    static Filter before(Consumer<Request> consumer) {
        return (request, resp) -> {
            consumer.accept(request);
            return resp;
        };
    }

    static Filter after(UnaryOperator<Response> function) {
        return (request, resp) -> resp.thenApply(function);
    }
}
