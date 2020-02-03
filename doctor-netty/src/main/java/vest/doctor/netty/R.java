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

    public R header(CharSequence name, Iterable<?> values) {
        if (values == null) {
            headers.remove(name);
        } else {
            headers.put(name, values);
        }
        return this;
    }

    public R header(CharSequence name, Object value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
        return this;
    }

    public R body(Object body) {
        if (body == this) {
            throw new IllegalArgumentException("may not set the body of the response to this instance");
        }
        this.body = body;
        return this;
    }

    public HttpResponseStatus status() {
        return status;
    }

    public Map<CharSequence, Object> headers() {
        return headers;
    }

    public Object body() {
        return body;
    }
}