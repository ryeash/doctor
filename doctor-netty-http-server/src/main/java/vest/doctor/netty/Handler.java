package vest.doctor.netty;

import java.util.concurrent.CompletionStage;

/**
 * Handles a request.
 */
@FunctionalInterface
public interface Handler {

    /**
     * Handle a request and produce a response.
     *
     * @param request the request
     * @return an asynchronously produced response
     */
    CompletionStage<Response> handle(Request request) throws Exception;

    /**
     * Create a synchronous handler.
     *
     * @param function a handler that produces a {@link Response} (rather than a
     *                 {@link CompletionStage})
     * @return a new {@link Handler}
     */
    static Handler sync(SynchronousHandler function) {
        return function;
    }
}
