package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;
import vest.doctor.Prioritized;


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
     * @return a {@link Publisher<HttpResponse> response publisher} to send a response to the client
     */
    Publisher<HttpResponse> handle(RequestContext requestContext, Throwable error);
}
