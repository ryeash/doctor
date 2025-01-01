package vest.doctor.apache.server;

import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;

public class HttpExceptionWithResponse extends RuntimeException {
    private final AsyncResponseBuilder response;

    public HttpExceptionWithResponse(AsyncResponseBuilder response) {
        this.response = response;
    }

    public HttpExceptionWithResponse(Throwable error, AsyncResponseBuilder response) {
        super(error);
        this.response = response;
    }

    public AsyncResponseBuilder response() {
        return response;
    }
}
