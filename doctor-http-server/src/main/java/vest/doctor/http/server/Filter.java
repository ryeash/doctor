package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.flow.Flo;

import java.util.Objects;
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
    Flo<?, Response> filter(Request request, FilterChain chain) throws Exception;

    /**
     * Create a filter that operates on the request before it is sent to a {@link Handler}.
     *
     * @param function the action to take on the request
     * @return a new before {@link Filter}
     */
    static Filter before(UnaryOperator<Request> function) {
        return new Before(Objects.requireNonNull(function));
    }

    /**
     * Create a filter that operates on the response after it has been returned from a {@link Handler}.
     *
     * @param function the function that will operate on (and return) the response object
     * @return a new after {@link Filter}
     */
    static Filter after(UnaryOperator<Response> function) {
        return new After(Objects.requireNonNull(function));
    }

    record Before(UnaryOperator<Request> function) implements Filter {
        @Override
        public Flo<?, Response> filter(Request request, FilterChain chain) throws Exception {
            return chain.next(function.apply(request));
        }
    }

    record After(UnaryOperator<Response> function) implements Filter {
        @Override
        public Flo<?, Response> filter(Request request, FilterChain chain) throws Exception {
            return chain.next(request).map(function);
        }
    }
}
