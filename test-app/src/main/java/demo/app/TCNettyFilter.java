package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.FilterChain;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.Path;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Singleton
@Path("/netty/*")
public class TCNettyFilter implements Filter {

    @Override
    public CompletionStage<Response> filter(Request request, FilterChain chain) throws Exception {
        if (Objects.equals(request.queryParam("halt"), "true")) {
            return request.createResponse()
                    .status(202)
                    .body(ResponseBody.of("halted"))
                    .wrapFuture();
        }

        Optional.ofNullable(request.queryParam("attr"))
                .ifPresent(a -> request.attribute("attr", a));

        request.headers().set("X-BEFORE-MATCH", true);
        request.headers().set("X-BEFORE-ROUTE", true);
        request.attribute("filter", true);
        return chain.next(request)
                .thenApply(r -> {
                    r.headers().set("X-AFTER-ROUTE", true);
                    return r;
                });
    }
}
