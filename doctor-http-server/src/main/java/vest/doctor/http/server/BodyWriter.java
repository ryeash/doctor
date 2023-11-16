package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Converts the return value of endpoint methods into responses.
 */
public interface BodyWriter extends Prioritized {

    /**
     * Convert the response object into an HTTP response.
     *
     * @param requestContext the request context
     * @param responseData   the data to write
     */
    CompletableFuture<RequestContext> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData);
}
