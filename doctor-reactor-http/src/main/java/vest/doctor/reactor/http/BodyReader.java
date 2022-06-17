package vest.doctor.reactor.http;

import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.RequestContext;

import java.util.concurrent.Flow;

/**
 * Deserializes request bodies into specific object types.
 */
public interface BodyReader extends Prioritized {

    /**
     * Read the body of the http request as the expected type.
     *
     * @param requestContext the request context
     * @param typeInfo       information about the target type for the deserialized data
     * @return the asynchronously deserialized request body
     */
    <T> Flow.Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo);
}
