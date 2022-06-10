package vest.doctor.http.server;

import java.util.concurrent.Flow;

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
    Flow.Publisher<Response> handle(RequestContext requestContext) throws Exception;
}
