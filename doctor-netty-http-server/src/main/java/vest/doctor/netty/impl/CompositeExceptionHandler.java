package vest.doctor.netty.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Prioritized;
import vest.doctor.netty.ExceptionHandler;
import vest.doctor.netty.HttpException;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ExceptionHandler} implementation that combines multiple handlers together
 * to provide robust handling of exceptions.
 */
public class CompositeExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CompositeExceptionHandler.class);
    private final List<ExceptionHandler> handlers = new LinkedList<>();

    public CompositeExceptionHandler addHandler(ExceptionHandler exceptionHandler) {
        if (!handlers.contains(exceptionHandler)) {
            handlers.add(exceptionHandler);
            handlers.sort(Prioritized.COMPARATOR);
        }
        return this;
    }

    @Override
    public Class<? extends Throwable> type() {
        return Throwable.class;
    }

    @Override
    public Response handle(Request request, Throwable error) {
        for (ExceptionHandler handler : handlers) {
            if (handler.type().isInstance(error)) {
                return handler.handle(request, error);
            }
        }
        return defaultWorkflow(request, error);
    }

    @Override
    public String toString() {
        return "CompositeExceptionHandler" + handlers;
    }

    private Response defaultWorkflow(Request request, Throwable error) {
        Objects.requireNonNull(error, "handle error was given a null error");
        log.warn("error during route execution; request uri: {}", request.uri(), error);

        Response response = request.createResponse();

        Throwable temp = error;
        for (int i = 0; i < 4 && temp != null; i++) {
            if (temp instanceof HttpException) {
                response.status(((HttpException) temp).status());
                response.body(ResponseBody.of(temp.getMessage()));
                return response;
            }
            temp = temp.getCause();
        }

        if (error instanceof IllegalArgumentException) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.body(ResponseBody.of(error.getMessage()));
        } else if (error instanceof InvocationTargetException && error.getCause() != null) {
            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.body(ResponseBody.of(error.getCause().getMessage()));
        } else {
            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.body(ResponseBody.of(error.getMessage()));
        }
        return response;
    }
}
