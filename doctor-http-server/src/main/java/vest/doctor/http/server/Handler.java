package vest.doctor.http.server;

import vest.doctor.flow.Flo;

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
    Flo<?, Response> handle(Request request) throws Exception;
}
