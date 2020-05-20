package demo.app;

import vest.doctor.netty.Filter;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TCNettyFilter implements Filter {

    @Override
    public CompletableFuture<Response> filter(Request request, CompletableFuture<Response> response) {
        if (Objects.equals(request.queryParam("halt"), "true")) {
            return request.createResponse()
                    .status(202)
                    .body(ResponseBody.of("halted"))
                    .wrapFuture();
        }

        request.headers().set("X-BEFORE-MATCH", true);
        request.headers().set("X-BEFORE-ROUTE", true);
        request.attribute("filter", true);
        return response.thenApply(r -> {
            r.headers().set("X-AFTER-ROUTE", true);
            return r;
        });
    }
}
