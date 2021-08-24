package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Unchecked exception that includes an HttpResponseStatus (defaults to 500 Internal Server Error).
 */
public class HttpException extends RuntimeException {

    private final HttpResponseStatus status;
    private final ResponseBody body;

    public HttpException(HttpResponseStatus status) {
        this(status, ResponseBody.empty(), null);
    }

    public HttpException(HttpResponseStatus status, String errorMessage) {
        this(status, ResponseBody.empty(), errorMessage);
    }

    public HttpException(HttpResponseStatus status, Throwable t) {
        this(status, ResponseBody.empty(), null, t);
    }

    public HttpException(HttpResponseStatus status, String errorMessage, Throwable t) {
        this(status, ResponseBody.empty(), errorMessage, t);
    }

    public HttpException(HttpResponseStatus status, ResponseBody body, String errorMessage, Throwable t) {
        super(errorMessage, t);
        this.status = status;
        this.body = body;
    }

    public HttpException(HttpResponseStatus status, ResponseBody body, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.body = body;
    }

    public HttpResponseStatus status() {
        return status;
    }

    public ResponseBody body() {
        return body;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + status + (getMessage() != null ? ": " + getMessage() : "");
    }
}