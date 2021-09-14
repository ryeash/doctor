package vest.doctor.http.server;

import vest.doctor.Prioritized;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

/**
 * A filter is inserted into the request processing stream to alter the inbound request
 * and/or the outbound response.
 */
@FunctionalInterface
public interface Filter extends Prioritized {

    /**
     * Filter a request/response.
     *
     * @param request the request that was received
     * @param chain   the next step in the filter chain
     * @return the response; possibly with new chained actions attached
     */
    CompletionStage<Response> filter(Request request, FilterChain chain) throws Exception;

    /**
     * Create a filter that operates on the request before it is sent to a {@link Handler}.
     *
     * @param function the action to take on the request
     * @return a new before {@link Filter}
     */
    static Filter before(UnaryOperator<Request> function) {
        Objects.requireNonNull(function);
        return new BeforeFilter(function);
    }

    /**
     * Create a filter that operates on the response after it has been returned from a {@link Handler}.
     *
     * @param function the function that will operate on (and return) the response object
     * @return a new after {@link Filter}
     */
    static Filter after(UnaryOperator<Response> function) {
        Objects.requireNonNull(function);
        return new AfterFilter(function);
    }

    class BeforeFilter implements Filter {

        private final UnaryOperator<Request> function;

        public BeforeFilter(UnaryOperator<Request> function) {
            this.function = function;
        }

        @Override
        public CompletionStage<Response> filter(Request request, FilterChain chain) throws Exception {
            return chain.next(function.apply(request));
        }
    }

    class AfterFilter implements Filter {

        private final UnaryOperator<Response> function;

        public AfterFilter(UnaryOperator<Response> function) {
            this.function = function;
        }

        @Override
        public CompletionStage<Response> filter(Request request, FilterChain chain) throws Exception {
            return chain.next(request).thenApply(function);
        }
    }
}
