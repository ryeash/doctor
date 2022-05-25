package vest.doctor.http.server;

import vest.doctor.reactive.Flo;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface Handler {
    /**
     * Process an HTTP request.
     *
     * @param requestContext the request context
     * @return an asynchronous Response result
     * @throws Exception for any error during processing
     */
    Flo<?, Response> handle(RequestContext requestContext) throws Exception;
}
