package vest.doctor.netty;

import vest.doctor.Prioritized;

/**
 * Responsible for deserializing request bodies.
 */
public interface BodyReader extends Prioritized {

    /**
     * Determine if this reader instance can deserialize the expected object from the http body.
     *
     * @param ctx            the request context
     * @param rawType        the raw type of the expected deserialized value
     * @param parameterTypes any parameter types expected for the deserialized value
     * @return true if this reader can read a body of the expected type
     */
    boolean handles(RequestContext ctx, Class<?> rawType, Class<?>... parameterTypes);

    /**
     * Read and deserialized the body of the http request to the expected type.
     *
     * @param ctx            the request context
     * @param rawType        the raw type of the expected deserialized value
     * @param parameterTypes any parameter types expected for the deserialized value
     * @return the deserialized value
     */
    <T> T read(RequestContext ctx, Class<T> rawType, Class<?>... parameterTypes);
}
