package vest.doctor.http.server;

import java.util.concurrent.CompletableFuture;

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
    CompletableFuture<RequestContext> next(RequestContext requestContext) throws Exception;
}
