package vest.doctor.http.server;

import java.util.concurrent.Flow;

/**
 * A handle to the next step in the filter chain.
 */
@FunctionalInterface
public interface FilterChain {

    /**
     * Call the next step in the filter chain using the given {@link Request}.
     *
     * @param requestContext the request to continue with
     * @return the future response
     * @throws Exception for any error processing the filter chain
     */
    Flow.Processor<?, Response> next(RequestContext requestContext) throws Exception;
}
