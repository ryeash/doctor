package vest.doctor.netty;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for building a response body that additionally needs to set status code or header values.
 */
public class R {

    /**
     * Create a 200 OK response.
     */
    public static R ok() {
        return new R(HttpResponseStatus.OK);
    }

    /**
     * Create a 200 OK response with a response body.
     */
    public static R ok(Object body) {
        return new R(HttpResponseStatus.OK).body(body);
    }

    /**
     * Create a response with the given status code.
     */
    public static R of(int status) {
        return new R(HttpResponseStatus.valueOf(status));
    }

    /**
     * Create a 400 Bad Request with a response body.
     */
    public static R badRequest(Object body) {
        return new R(HttpResponseStatus.BAD_REQUEST).body(body);
    }

    private HttpResponseStatus status;
    private final Map<CharSequence, Object> headers;
    private Object body;

    private R(HttpResponseStatus status) {
        this.status = status;
        this.headers = new HashMap<>(8);
    }

    /**
     * Set the response status code.
     *
     * @param status the status code
     * @return this object
     */
    public R status(int status) {
        this.status = HttpResponseStatus.valueOf(status);
        return this;
    }

    /**
     * Set the response status code.
     *
     * @param status the status code
     * @return this object
     */
    public R status(HttpResponseStatus status) {
        this.status = Objects.requireNonNull(status);
        return this;
    }

    /**
     * Set the response status code with custom status message.
     *
     * @param status  the status code
     * @param message thw custom status message
     * @return this object
     */
    public R status(int status, String message) {
        Objects.requireNonNull(message);
        this.status = HttpResponseStatus.valueOf(status, message);
        return this;
    }

    /**
     * Set a response header's value(s).
     *
     * @param name   the header to set
     * @param values the values to set
     * @return this object
     */
    public R header(CharSequence name, Iterable<?> values) {
        if (values == null) {
            headers.remove(name);
        } else {
            headers.put(name, values);
        }
        return this;
    }

    /**
     * Set a response header.
     *
     * @param name  the header name
     * @param value the header value
     * @return this object
     */
    public R header(CharSequence name, Object value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
        return this;
    }

    /**
     * Set the response body.
     *
     * @param body the body
     * @return this object
     */
    public R body(Object body) {
        if (body == this) {
            throw new IllegalArgumentException("may not set the body of the response to this instance");
        }
        this.body = body;
        return this;
    }

    /**
     * Get the current status of the response
     *
     * @return the status
     */
    public HttpResponseStatus status() {
        return status;
    }

    /**
     * Get the headers of the response.
     *
     * @return the headers
     */
    public Map<CharSequence, Object> headers() {
        return headers;
    }

    /**
     * Get the body of the response.
     *
     * @return the body
     */
    public Object body() {
        return body;
    }

    Response applyTo(Response response) {
        response.status(status);
        headers.forEach(response::header);
        return response;
    }
}