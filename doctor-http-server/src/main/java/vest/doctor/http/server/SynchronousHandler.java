package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Synchronous version of the {@link Handler} interface.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default CompletionStage<Response> handle(Request request) {
        CompletableFuture<ByteBuf> futureBody = request.body().completionFuture();
        return futureBody.thenCombineAsync(CompletableFuture.completedFuture(request), (body, req) -> {
            try {
                return handleSync(req, body);
            } finally {
                if (body.refCnt() > 0) {
                    body.release();
                }
            }
        }, request.pool());
    }

    /**
     * Handle the request synchronously.
     *
     * @param request the request
     * @return a {@link Response}
     */
    Response handleSync(Request request, ByteBuf body);
}
