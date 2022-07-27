package vest.doctor.http.server;

import java.util.concurrent.Flow;

/**
 * A handle to the next step in the filter chain.
 */
@FunctionalInterface
public interface FilterChain {

    /**
     * Call the next step in the filter chain using the given {@link RequestContext}.
     *
     * @param requestContext the request context to continue with
     * @return the response publisher
     * @throws Exception for any error processing the filter chain
     */
    Flow.Publisher<Response> next(RequestContext requestContext) throws Exception;
}
