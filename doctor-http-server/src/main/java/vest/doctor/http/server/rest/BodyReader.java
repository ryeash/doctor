package vest.doctor.http.server.rest;

import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.Request;

import java.util.concurrent.CompletableFuture;

/**
 * Responsible for deserializing request bodies.
 */
public interface BodyReader extends Prioritized {

    /**
     * Determine if this reader instance can deserialize the expected object from the http body.
     * If this method returns true, the {@link #read(Request, TypeInfo)} method will be called to
     * deserialize the request body data.
     *
     * @param request  the request context
     * @param typeInfo information about the target type for the deserialized data
     * @return true if this reader can read a body of the expected type
     */
    boolean canRead(Request request, TypeInfo typeInfo);

    /**
     * Read the body of the http request as the expected type.
     *
     * @param request  the request context
     * @param typeInfo information about the target type for the deserialized data
     * @return the deserialized value
     */
    <T> CompletableFuture<T> read(Request request, TypeInfo typeInfo);
}
