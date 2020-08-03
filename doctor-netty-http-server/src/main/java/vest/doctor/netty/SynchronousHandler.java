package vest.doctor.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Synchronized version of the {@link Handler} interface.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default CompletionStage<Response> handle(Request request) {
        return CompletableFuture.completedFuture(handleSync(request));
    }

    /**
     * Handle the request synchronously.
     *
     * @param request the request
     * @return a {@link Response}
     */
    Response handleSync(Request request);
}
