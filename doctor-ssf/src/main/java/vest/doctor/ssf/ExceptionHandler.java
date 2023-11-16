package vest.doctor.ssf;

import java.util.concurrent.CompletableFuture;

public interface ExceptionHandler {
    CompletableFuture<RequestContext> handle(RequestContext requestContext, Throwable error);
}
