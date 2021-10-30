package vest.doctor.http.server.rest;

import vest.doctor.Prioritized;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.util.concurrent.CompletableFuture;

/**
 * Responsible for converting the result of endpoint methods into {@link ResponseBody response bodies}.
 */
public interface BodyWriter extends Prioritized {

    /**
     * Determine if this writer instance handles writing of the object type.
     *
     * @param response     the response context
     * @param responseData the response object from the endpoint method
     * @return true if this body writer can handle writing the response object
     */
    boolean canWrite(Response response, Object responseData);

    /**
     * Convert the response data into a {@link ResponseBody}.
     *
     * @param response     the response object
     * @param responseData the data to write
     * @return a future indicating the asynchronous creation of the response body
     */
    // TODO: flow this
    CompletableFuture<ResponseBody> write(Response response, Object responseData);
}
