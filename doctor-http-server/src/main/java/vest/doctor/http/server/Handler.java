package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface Handler {

    /**
     * Process an HTTP request.
     *
     * @param requestContext the request context
     * @return an asynchronous Response result
     * @throws Exception for any error during processing
     */
    CompletableFuture<RequestContext> handle(RequestContext requestContext) throws Exception;

    /**
     * Create a request Handler with a synchronous function. The request body will be buffered
     * into a single {@link ByteBuf} and automatically {@link ByteBuf#release() released} after the function is called.
     *
     * @param consumer the request handler
     * @return a new handler
     */
    static Handler sync(Consumer<RequestContext> consumer) {
        return requestContext -> CompletableFuture.supplyAsync(() -> {
            consumer.accept(requestContext);
            return requestContext;
        }, requestContext.pool());
    }
}
