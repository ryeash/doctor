package vest.doctor.netty;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class R {

    public static R ok() {
        return new R(HttpResponseStatus.OK);
    }

    public static R ok(Object body) {
        return new R(HttpResponseStatus.OK).body(body);
    }

    public static R of(int status) {
        return new R(HttpResponseStatus.valueOf(status));
    }

    public static R badRequest(Object body) {
        return new R(HttpResponseStatus.BAD_REQUEST).body(body);
    }

    private HttpResponseStatus status;
    private Map<CharSequence, Object> headers;
    private Object body;

    private R(HttpResponseStatus status) {
        this.status = status;
        this.headers = new HashMap<>(8);
    }

    public R status(int status) {
        this.status = HttpResponseStatus.valueOf(status);
        return this;
    }

    public R status(HttpResponseStatus status) {
        this.status = Objects.requireNonNull(status);
        return this;
    }

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