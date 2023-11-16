package vest.doctor.ssf;

import java.util.concurrent.CompletableFuture;

public interface RequestContext {
    Request request();

    Response response();

    void attribute(String name, Object value);

    <T> T attribute(String name);

    default CompletableFuture<RequestContext> send() {
        return CompletableFuture.completedFuture(this);
    }
}
