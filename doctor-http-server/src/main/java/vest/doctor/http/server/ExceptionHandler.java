package vest.doctor.http.server;

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
     * Handle the exception.
     *
     * @param request the request that caused the exception
     * @param error   the error from the handler
     * @return a {@link Response} to send to the client
     */
    Response handle(Request request, Throwable error);
}
