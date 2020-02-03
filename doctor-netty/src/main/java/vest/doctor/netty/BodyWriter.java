package vest.doctor.netty;

import vest.doctor.Prioritized;

/**
 * Responsible for serialized response bodies.
 */
public interface BodyWriter extends Prioritized {

    /**
     * Determine if this writer handler writing of the object type.
     *
     * @param ctx      the request context
     * @param response the response object from the endpoint method
     * @return true if this body writer can handle writing the response object
     */
    boolean handles(RequestContext ctx, Object response);

    /**
     * Serialize the response object into the request context (using one of the {@link RequestContext#responseBody} methods).
     * It is not the responsibility of writers to call {@link RequestContext#complete()}.
     *
     * @param ctx      the request context
     * @param response the response object from the endpoint method
     */
    void write(RequestContext ctx, Object response);
}
