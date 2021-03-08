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
     *
     * @param request  the request context
     * @param typeInfo information about the target type for the deserialized data
     * @return true if this reader can read a body of the expected type
     */
    boolean handles(Request request, TypeInfo typeInfo);

    /**
     * Read and deserialize the body of the http request to the expected type.
     *
     * @param request  the request context
     * @param typeInfo information about the target type for the deserialized data
     * @return the deserialized value
     */
    <T> CompletableFuture<T> read(Request request, TypeInfo typeInfo);
}
