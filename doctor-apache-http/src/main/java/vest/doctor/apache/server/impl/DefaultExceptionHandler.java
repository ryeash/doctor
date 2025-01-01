package vest.doctor.apache.server.impl;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.apache.server.ExceptionHandler;
import vest.doctor.apache.server.HttpExceptionWithResponse;
import vest.doctor.apache.server.RequestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DefaultExceptionHandler implements ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    private final List<ExceptionHandler> handlers = new ArrayList<>();

    public void add(ExceptionHandler exceptionHandler) {
        this.handlers.add(exceptionHandler);
        Collections.sort(handlers);
    }

    @Override
    public AsyncResponseBuilder handle(RequestContext ctx, Throwable error) {
        log.error("[{}] error handling request: {} {}", ctx.traceId(), ctx.request().getMethod(), ctx.request().getPath(), error);
        try {
            for (ExceptionHandler handler : handlers) {
                AsyncResponseBuilder handle = handler.handle(ctx, error);
                if (handle != null) {
                    return handle;
                }
            }
        } catch (Throwable t) {
            log.error("error handling error", t);
        }
        return defaultHandler(ctx, error);
    }

    public static AsyncResponseBuilder defaultHandler(RequestContext ctx, Throwable error) {
        Throwable temp = error;
        for (int i = 0; i < 10; i++) {
            if (temp == null) {
                break;
            }
            if (temp instanceof HttpExceptionWithResponse re) {
                return re.response();
            }
            temp = temp.getCause();
        }
        return AsyncResponseBuilder.create(500)
                .setHeader("X-Trace", Optional.ofNullable(ctx).map(RequestContext::traceId).orElse("unknown"))
                .setEntity("{\"error\":\"" + error.getMessage() + "\"}", ContentType.APPLICATION_JSON);
    }
}
