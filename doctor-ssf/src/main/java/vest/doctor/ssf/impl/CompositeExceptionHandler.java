package vest.doctor.ssf.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.rx.SinglePublisher;
import vest.doctor.ssf.ExceptionHandler;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.Response;
import vest.doctor.ssf.SSFException;
import vest.doctor.ssf.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow;

public class CompositeExceptionHandler implements ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    private final List<ExceptionHandler> list = new LinkedList<>();

    public void add(ExceptionHandler exceptionHandler) {
        list.add(exceptionHandler);
    }

    @Override
    public Flow.Publisher<Response> handle(Request request, Throwable error) {
        for (ExceptionHandler exceptionHandler : list) {
            Flow.Publisher<Response> handled = exceptionHandler.handle(request, error);
            if (handled != null) {
                return handled;
            }
        }
        return defaultWorkflow(request, error);
    }

    public static Flow.Publisher<Response> defaultWorkflow(Request request, Throwable error) {
        Response response = Response.of(Status.INTERNAL_SERVER_ERROR);
        response.setHeader(BaseMessage.CONTENT_TYPE, BaseMessage.TEXT_PLAIN);
        log.error("error executing request {} {}", request.method(), request.uri(), error);
        Throwable temp = error;
        for (int i = 0; i < 10 && temp != null; i++) {
            if (temp instanceof SSFException je) {
                response.status(je.getStatus());
                response.body(je.getMessage());
                return new SinglePublisher<>(response, request.pool());
            }
            temp = temp.getCause();
        }
        response.body(error.getMessage());
        return new SinglePublisher<>(response, request.pool());
    }
}