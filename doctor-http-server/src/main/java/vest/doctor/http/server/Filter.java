package vest.doctor.http.server;

import vest.doctor.Prioritized;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A filter is inserted into the request processing stream to alter the inbound request
 * and/or the outbound response.
 */
@FunctionalInterface
public interface Filter extends Prioritized {

    /**
     * Filter a request/response.
     *
     * @param requestContext the request that was received
     * @param chain          the next step in the filter chain
     * @return the response; possibly with new chained actions attached
     */
    CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain) throws Exception;

    /**
     * Create a filter that operates on the request before it is sent to a {@link Handler}.
     *
     * @param consumer the action to take on the request
     * @return a new before {@link Filter}
     */
    static Filter before(Consumer<RequestContext> consumer) {
        return new Before(Objects.requireNonNull(consumer));
    }

    /**
     * Create a filter that operates on the response after it has been returned from a {@link Handler}.
     *
     * @param consumer the consumer that will operate on the response object
     * @return a new after {@link Filter}
     */
    static Filter after(Consumer<RequestContext> consumer) {
        return new After(Objects.requireNonNull(consumer));
    }

    record Before(Consumer<RequestContext> consumer) implements Filter {
        @Override
        public CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain) throws Exception {
            consumer.accept(requestContext);
            return chain.next(requestContext);
        }
    }

    record After(Consumer<RequestContext> consumer) implements Filter {
        @Override
        public CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain) throws Exception {
            return chain.next(requestContext)
                    .thenApply(ctx -> {
                        consumer.accept(ctx);
                        return ctx;
                    });
        }
    }
}
