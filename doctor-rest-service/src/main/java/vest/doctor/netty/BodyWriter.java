package vest.doctor.netty;

import vest.doctor.Prioritized;

import java.util.concurrent.CompletableFuture;

/**
 * Responsible for serializing response bodies.
 */
public interface BodyWriter extends Prioritized {

    /**
     * Determine if this writer instance handles writing of the object type.
     *
     * @param response     the response context
     * @param responseData the response object from the endpoint method
     * @return true if this body writer can handle writing the response object
     */
    boolean handles(Response response, Object responseData);

    /**
     * Write the response data into the response.
     *
     * @param response     the response object
     * @param responseData the data to write
     * @return a future indicating the asynchronous completion of the write operation
     */
    CompletableFuture<ResponseBody> write(Response response, Object responseData);
}
