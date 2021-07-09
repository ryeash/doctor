package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Prioritized;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.RxExceptionHandler;
import vest.doctor.http.server.RxRequest;
import vest.doctor.http.server.RxResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * {@link ExceptionHandler} implementation that combines multiple handlers together
 * to provide robust handling of exceptions.
 */
public class RxCompositeExceptionHandler implements RxExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RxCompositeExceptionHandler.class);
    private final List<RxExceptionHandler> handlers = new LinkedList<>();

    public void addHandler(RxExceptionHandler exceptionHandler) {
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
    public RxResponse handle(RxRequest request, Throwable error) {
        for (RxExceptionHandler handler : handlers) {
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

    private RxResponse defaultWorkflow(RxRequest request, Throwable error) {
        Objects.requireNonNull(error, "handle error was given a null error");
        log.warn("error during route execution; request uri: {}", request.uri(), error);

        RxResponse response = request.createResponse();

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
