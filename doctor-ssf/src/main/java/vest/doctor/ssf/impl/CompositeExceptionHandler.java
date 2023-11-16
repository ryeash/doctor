package vest.doctor.ssf.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.ssf.ExceptionHandler;
import vest.doctor.ssf.SSFException;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.Response;
import vest.doctor.ssf.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompositeExceptionHandler implements ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    private final List<ExceptionHandler> list = new LinkedList<>();

    public void add(ExceptionHandler exceptionHandler) {
        list.add(exceptionHandler);
    }

    @Override
    public CompletableFuture<RequestContext> handle(RequestContext requestContext, Throwable error) {
        for (ExceptionHandler exceptionHandler : list) {
            CompletableFuture<RequestContext> handled = exceptionHandler.handle(requestContext, error);
            if (handled != null) {
                return handled;
            }
        }
        return defaultWorkflow(requestContext, error);
    }

    public static CompletableFuture<RequestContext> defaultWorkflow(RequestContext requestContext, Throwable error) {
        Request request = requestContext.request();
        Response response = requestContext.response();
        response.setHeader(Headers.CONTENT_TYPE, Headers.TEXT_PLAIN);
        log.error("error executing request {} {}", request.method(), request.uri(), error);
        Throwable temp = error;
        for (int i = 0; i < 10 && temp != null; i++) {
            if (temp instanceof SSFException je) {
                response.status(je.getStatus());
                response.body(je.getMessage());
                return requestContext.send();
            }
            temp = temp.getCause();
        }
        response.status(Status.INTERNAL_SERVER_ERROR);
        response.body(error.getMessage());
        return requestContext.send();
    }
}