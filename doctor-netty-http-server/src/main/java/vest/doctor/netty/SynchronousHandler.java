package vest.doctor.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default CompletionStage<Response> handle(Request request) {
        return CompletableFuture.completedFuture(handleSync(request));
    }

    Response handleSync(Request request);
}
