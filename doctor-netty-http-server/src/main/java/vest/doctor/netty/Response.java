package vest.doctor.netty;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.CompletableFuture;

public interface Response {

    Response status(HttpResponseStatus status);

    Response status(int status);

    Response status(int status, String reasonString);

    HttpResponseStatus status();

    Response header(CharSequence name, Object value);

    HttpHeaders headers();

    Response setCookie(String name, String value);

    Response body(ResponseBody body);

    ResponseBody body();

    Request request();

    default CompletableFuture<Response> wrapFuture() {
        return CompletableFuture.completedFuture(this);
    }
}
