package vest.doctor.netty;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Handler {

    CompletableFuture<Response> handle(Request request);

    static Handler sync(SynchronousHandler function) {
        return function;
    }
}
