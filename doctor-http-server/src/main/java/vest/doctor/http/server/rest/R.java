package vest.doctor.http.server.rest;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class for building an endpoint response.
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
     * Create a response with the given status code.
     */
    public static R of(HttpResponseStatus status) {
        return new R(status);
    }

    /**
     * Create a 400 Bad Request with a response body.
     */
    public static R badRequest(Object body) {
        return new R(HttpResponseStatus.BAD_REQUEST).body(body);
    }

    private HttpResponseStatus status;
    private final HttpHeaders headers;
    private Object body;

    private R(HttpResponseStatus status) {
        this.status = status;
        this.headers = new DefaultHttpHeaders();
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
     * Set a response header's value(s). If values is null, the header will be removed.
     *
     * @param name   the header to set
     * @param values the values to set
     * @return this object
     */
    public R header(CharSequence name, Iterable<?> values) {
        if (values == null) {
            headers.remove(name);
        } else {
            headers.set(name, values);
        }
        return this;
    }

    /**
     * Set a response header. If value is null, the header will be removed.
     *
     * @param name  the header name
     * @param value the header value
     * @return this object
     */
    public R header(CharSequence name, Object value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.set(name, value);
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
    public HttpHeaders headers() {
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

    /**
     * Internal use. Apply the status and headers from this object to the given response.
     *
     * @param response the response to apply the data to
     * @return the response
     */
    Response applyTo(Response response) {
        response.status(status);
        for (Map.Entry<String, String> entry : headers) {
            response.header(entry.getKey(), entry.getValue());
        }
        return response;
    }

    /**
     * Create a new file response.
     *
     * @param request       the request object
     * @param rootDirectory the root directory to search for the file in; the filePath
     *                      will be validated against this root and traversal attacks will be
     *                      prevented
     * @param filePath      the path to the file (from the root directory)
     * @return a new response object for the file
     */
    public static R file(Request request, String rootDirectory, String filePath) {
        java.nio.file.Path rootPath = new File(rootDirectory).getAbsoluteFile().toPath();

        File file = new File(new File(rootDirectory), filePath).getAbsoluteFile();
        Path filepath = file.toPath();
        if (!filepath.startsWith(rootPath)) {
            return R.of(HttpResponseStatus.FORBIDDEN)
                    .body("invalid path: " + filePath);
        }
        if (!file.canRead() || file.isHidden() || !file.isFile()) {
            return R.of(HttpResponseStatus.NOT_FOUND)
                    .body("file does not exist: " + filePath);
        }
        long modified = file.lastModified();

        Long ifModifiedSince = request.headers().getTimeMillis(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (!isModified(modified, ifModifiedSince)) {
            return new R(HttpResponseStatus.NOT_MODIFIED).body(null);
        }
        R r = R.ok()
                .header(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
                .body(ResponseBody.sendFile(file));

        // only set these headers if they don't exists -> allows them to be overwritten by user code
        if (!r.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            r.header(HttpHeaderNames.CONTENT_TYPE, Utils.getContentType(file));
        }
        if (!r.headers().contains(HttpHeaderNames.LAST_MODIFIED)) {
            r.header(HttpHeaderNames.LAST_MODIFIED, new Date(file.lastModified()));
        }
        if (file.getName().endsWith(".gz")) {
            r.header(HttpHeaderNames.CONTENT_ENCODING, HttpHeaderValues.GZIP);
            r.header(HttpHeaderNames.CONTENT_TYPE, "application/gzip");
        } else {
            r.header(HttpHeaderNames.CONTENT_ENCODING, "");
        }
        return r;
    }

    private static boolean isModified(long modified, Long ifModifiedSince) {
        if (ifModifiedSince != null) {
            long ifModifiedSinceDateSeconds = ifModifiedSince / 1000;
            long fileLastModifiedSeconds = modified / 1000;
            return ifModifiedSinceDateSeconds != fileLastModifiedSeconds;
        }
        return true;
    }
}