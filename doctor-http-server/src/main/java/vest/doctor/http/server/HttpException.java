package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Unchecked exception that includes an HttpResponseStatus (defaults to 500 Internal Server Error).
 */
public class HttpException extends RuntimeException {

    private HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;

    public HttpException(String errorMessage) {
        super(errorMessage);
    }

    public HttpException(String errorMessage, Throwable t) {
        super(errorMessage, t);
    }

    public HttpException(Throwable error) {
        super(error);
    }

    public HttpException(HttpResponseStatus status) {
        super();
        this.status = status;
    }

    public HttpException(HttpResponseStatus status, String errorMessage) {
        super(errorMessage);
        this.status = status;
    }

    public HttpException(HttpResponseStatus status, Throwable t) {
        super(t);
        this.status = status;
    }

    public HttpException(HttpResponseStatus status, String errorMessage, Throwable t) {
        super(errorMessage, t);
        this.status = status;
    }

    public HttpException(int status) {
        super();
        this.status = HttpResponseStatus.valueOf(status);
    }

    public HttpException(int status, Throwable t) {
        super(t);
        this.status = HttpResponseStatus.valueOf(status);
    }

    public HttpException(int status, String errorMessage) {
        super(errorMessage);
        this.status = HttpResponseStatus.valueOf(status);
    }

    public HttpException(int status, String errorMessage, Throwable t) {
        super(errorMessage, t);
        this.status = HttpResponseStatus.valueOf(status);
    }

    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + status + (getMessage() != null ? ": " + getMessage() : "");
    }
}