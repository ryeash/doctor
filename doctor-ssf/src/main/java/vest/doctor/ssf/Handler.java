package vest.doctor.ssf;

import java.util.concurrent.CompletableFuture;

public interface Handler {

    CompletableFuture<RequestContext> handle(RequestContext requestContext);
}
