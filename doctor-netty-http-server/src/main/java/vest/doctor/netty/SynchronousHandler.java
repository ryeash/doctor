package vest.doctor.netty;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default CompletableFuture<Response> handle(Request request) {
        request.body().ignored().join();
        return CompletableFuture.completedFuture(handleSync(request));
    }

    Response handleSync(Request request);
}
