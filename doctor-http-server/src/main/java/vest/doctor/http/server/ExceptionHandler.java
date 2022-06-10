package vest.doctor.http.server;

import vest.doctor.Prioritized;

import java.util.concurrent.Flow;


/**
 * Handles exceptions thrown by {@link Handler}s.
 */
public interface ExceptionHandler extends Prioritized {

    /**
     * The type of exception that is handled by the implementation.
     */
    Class<? extends Throwable> type();

    /**
     * Handle an exception caused by executing a {@link Handler}.
     * The exception may have been thrown from the handler, or returned as an exceptionally completing
     * future, e.g. {@link java.util.concurrent.CompletableFuture#completeExceptionally(Throwable)}.
     *
     * @param requestContext the requestContext that caused the exception
     * @param error          the error from the handler
     * @return a {@link Flow.Publisher response publisher} to send a response to the client
     */
    Flow.Publisher<Response> handle(RequestContext requestContext, Throwable error);
}
