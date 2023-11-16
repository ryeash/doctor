package vest.doctor.ssf;

import java.util.concurrent.CompletableFuture;

public interface FilterChain {

    CompletableFuture<RequestContext> next(RequestContext requestContext);
}
