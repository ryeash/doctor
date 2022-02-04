package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;

/**
 * Converts the return value of endpoint methods into responses.
 */
public interface BodyWriter extends Prioritized {

    /**
     * Convert the response object into an HTTP response.
     *
     * @param requestContext the request context
     * @param responseData   the data to write
     * @return a publisher indicating the asynchronous completion of the response
     */
    Publisher<HttpResponse> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData);
}
