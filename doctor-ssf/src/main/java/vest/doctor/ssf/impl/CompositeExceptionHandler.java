package vest.doctor.ssf.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.sleipnir.http.Headers;
import vest.doctor.sleipnir.http.HttpException;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.ssf.ExceptionHandler;
import vest.doctor.ssf.RequestContext;

import java.util.LinkedList;
import java.util.List;

public class CompositeExceptionHandler implements ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    private final List<ExceptionHandler> list = new LinkedList<>();

    public void add(ExceptionHandler exceptionHandler) {
        list.add(exceptionHandler);
    }

    @Override
    public void handle(RequestContext request, Throwable error) {
        if (request.committed()) {
            log.warn("response has already been sent, unable to execute error handling for {}", request, error);
        }
        for (ExceptionHandler exceptionHandler : list) {
            exceptionHandler.handle(request, error);
            if (request.committed()) {
                return;
            }
        }
        defaultWorkflow(request, error);
    }

    public static void defaultWorkflow(RequestContext ctx, Throwable error) {
        log.error("error executing request {} {}", ctx.method(), ctx.uri(), error);
        ctx.status(Status.INTERNAL_SERVER_ERROR);
        ctx.responseHeaders().set(Headers.CONTENT_TYPE, Headers.TEXT_PLAIN);
        ctx.responseBody(error.getMessage());
        Throwable temp = error;
        for (int i = 0; i < 10 && temp != null; i++) {
            if (temp instanceof HttpException je) {
                ctx.status(je.getStatus());
                ctx.responseBody(je.getMessage());
                break;
            }
            temp = temp.getCause();
        }
        ctx.send();
    }
}