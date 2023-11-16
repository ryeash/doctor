package vest.doctor.http.server;

import vest.doctor.Prioritized;


/**
 * Handles exceptions thrown by {@link Handler}s.
 */
public interface ExceptionHandler extends Prioritized {

    /**
     * The type of exception that is handled by this implementation.
     */
    Class<? extends Throwable> type();

    /**
     * Handle an exception caused by executing a {@link Handler}.
     *
     * @param requestContext the requestContext that caused the exception
     * @param error          the error from the handler
     * @return a {@link Response} to send to the client
     */
    RequestContext handle(RequestContext requestContext, Throwable error);
}
