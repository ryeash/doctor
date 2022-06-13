package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Prioritized;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ExceptionHandler} implementation that combines multiple handlers together
 * to provide robust handling of exceptions.
 */
public final class CompositeExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CompositeExceptionHandler.class);
    private final List<ExceptionHandler> handlers = new LinkedList<>();

    public void addHandler(ExceptionHandler exceptionHandler) {
        if (!handlers.contains(exceptionHandler)) {
            handlers.add(exceptionHandler);
            handlers.sort(Prioritized.COMPARATOR);
        }
    }

    @Override
    public Class<? extends Throwable> type() {
        return Throwable.class;
    }

    @Override
    public Response handle(RequestContext requestContext, Throwable error) {
        for (ExceptionHandler handler : handlers) {
            if (handler.type().isInstance(error)) {
                return handler.handle(requestContext, error);
            }
        }
        return defaultWorkflow(requestContext, error);
    }

    @Override
    public String toString() {
        return "CompositeExceptionHandler" + handlers;
    }

    private Response defaultWorkflow(RequestContext requestContext, Throwable error) {
        if (error == null) {
            log.error("null exception somehow", new Exception());
            return requestContext.response().status(500).body(ResponseBody.empty());
        }
        Objects.requireNonNull(error, "handle error was given a null error");
        log.error("error during route execution; request uri: {}", requestContext.request().uri(), error);

        Throwable temp = error;
        for (int i = 0; i < 7 && temp != null; i++) {
            if (temp instanceof HttpException http) {
                return requestContext.response()
                        .status(http.status())
                        .body(http.body());
            }
            temp = temp.getCause();
        }

        HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String body = error.toString();
        if (error instanceof IllegalArgumentException) {
            status = HttpResponseStatus.BAD_REQUEST;
        } else if (error instanceof InvocationTargetException && error.getCause() != null) {
            body = error.getCause().getMessage();
        }
        return requestContext.response()
                .status(status)
                .body(ResponseBody.of(body));
    }
}
