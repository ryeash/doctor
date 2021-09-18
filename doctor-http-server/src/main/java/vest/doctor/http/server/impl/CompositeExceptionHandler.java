package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Prioritized;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.Request;
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
public class CompositeExceptionHandler implements ExceptionHandler {

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
        if (error == null) {
            log.error("null exception somehow", new Exception());
            return request.createResponse()
                    .status(500);
        }
        Objects.requireNonNull(error, "handle error was given a null error");
        log.error("error during route execution; request uri: {}", request.uri(), error);

        Response response = request.createResponse();

        Throwable temp = error;
        for (int i = 0; i < 7 && temp != null; i++) {
            if (temp instanceof HttpException) {
                response.status(((HttpException) temp).status());
                response.body(((HttpException) temp).body());
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
