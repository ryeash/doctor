package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;

import java.util.concurrent.Flow;

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
    Flow.Publisher<Response> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData);
}
