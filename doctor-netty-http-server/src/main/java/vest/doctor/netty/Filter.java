package vest.doctor.netty;

import vest.doctor.Prioritized;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
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
     * @param request  the request that was received
     * @param response the future response (will be completed by the selected {@link Handler}
     *                 in the {@link vest.doctor.netty.impl.Router})
     * @return the response; possibly with new chained actions attached
     */
    CompletionStage<Response> filter(Request request, CompletionStage<Response> response);

    /**
     * Create a filter that operates on the request before it is sent to a {@link Handler}.
     *
     * @param consumer the action to take on the request
     * @return a new before {@link Filter}
     */
    static Filter before(Consumer<Request> consumer) {
        return (request, resp) -> {
            consumer.accept(request);
            return resp;
        };
    }

    /**
     * Create a filter that operates on the response after it has been returned from a {@link Handler}.
     *
     * @param function the function that will operate on (and return) the response object
     * @return a new after {@link Filter}
     */
    static Filter after(UnaryOperator<Response> function) {
        return (request, resp) -> resp.thenApply(function);
    }
}
