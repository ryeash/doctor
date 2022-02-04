package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;

/**
 * A handle to the filter chain for a specific request context.
 */
@FunctionalInterface
public interface FilterChain {

    /**
     * Continue the request by passing the request context to the next filter.
     * The last step in the filter chain will be the {@link Handler} for the request.
     *
     * @param requestContext the request context
     * @return the asynchronously produced response
     * @throws Exception for any error during processing
     */
    Publisher<HttpResponse> next(RequestContext requestContext) throws Exception;
}
