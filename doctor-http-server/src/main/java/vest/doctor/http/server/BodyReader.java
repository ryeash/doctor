package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;

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
    <T> T read(RequestContext requestContext, TypeInfo typeInfo);
}
