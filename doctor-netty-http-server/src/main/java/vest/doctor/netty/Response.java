package vest.doctor.netty;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Models an HTTP response
 */
public interface Response {

    /**
     * Set the response status.
     *
     * @param status the status
     * @return this object
     */
    Response status(HttpResponseStatus status);

    /**
     * Set the response status.
     *
     * @param status the status
     * @return this object
     */
    Response status(int status);

    /**
     * Set the response status.
     *
     * @param status       the status
     * @param reasonString the status message
     * @return this object
     */
    Response status(int status, String reasonString);

    /**
     * Get the response status.
     */
    HttpResponseStatus status();

    /**
     * Set a header.
     *
     * @param name  the name of the header
     * @param value the value of the header
     * @return this object
     */
    Response header(CharSequence name, Object value);

    /**
     * Get the response {@link HttpHeaders}.
     *
     * @return the headers
     */
    HttpHeaders headers();

    /**
     * Set the response body.
     *
     * @param body the body to send in the response
     * @return this object
     */
    Response body(ResponseBody body);

    /**
     * Get the response body.
     */
    ResponseBody body();

    /**
     * Get the request that this response is paired to.
     */
    Request request();

    /**
     * Wrap this response in a completed {@link CompletableFuture}.
     */
    default CompletableFuture<Response> wrapFuture() {
        return CompletableFuture.completedFuture(this);
    }
}
