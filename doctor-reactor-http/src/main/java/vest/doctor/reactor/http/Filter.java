package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import vest.doctor.Prioritized;

/**
 * A filter is inserted into the request processing stream to alter the inbound request
 * and/or the outbound response.
 */
@FunctionalInterface
public interface Filter extends Prioritized {

    /**
     * Filter a request/response.
     *
     * @param requestContext the request context
     * @param chain          the next step in the filter chain
     * @return the response publisher
     */
    Publisher<HttpResponse> filter(RequestContext requestContext, FilterChain chain) throws Exception;
}
