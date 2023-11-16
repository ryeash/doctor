package demo.app.service;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.FilterChain;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.ResponseBody;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TestFilter implements Filter {
    @Override
    public CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain) throws Exception {
        Request request = requestContext.request();
        if (Objects.equals(request.queryParam("halt"), "true")) {
            requestContext.response().status(202).body(ResponseBody.of("halted"));
            return CompletableFuture.completedFuture(requestContext);
        }

        requestContext.attribute("test.attribute", "value");
        return chain.next(requestContext)
                .thenApply(r -> {
                    r.response().header("X-After", "true");
                    return r;
                });
    }
}
