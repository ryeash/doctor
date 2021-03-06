package vest.doctor.http.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Synchronous version of the {@link Handler} interface.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default CompletionStage<Response> handle(Request request) {
        return request.body()
                .completionFuture()
                .thenCombineAsync(CompletableFuture.completedFuture(request), (body, req) -> handleSync(req), request.pool());
    }

    /**
     * Handle the request synchronously.
     *
     * @param request the request
     * @return a {@link Response}
     */
    Response handleSync(Request request);
}
