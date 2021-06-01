package vest.doctor.http.server;

import java.util.concurrent.CompletionStage;

/**
 * Handles an HTTP request.
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
}
