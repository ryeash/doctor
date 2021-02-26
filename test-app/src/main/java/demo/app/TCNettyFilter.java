package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.netty.Filter;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Singleton
public class TCNettyFilter implements Filter {

    @Override
    public CompletionStage<Response> filter(Request request, CompletionStage<Response> response) {
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
        return response.thenApply(r -> {
            r.headers().set("X-AFTER-ROUTE", true);
            return r;
        });
    }
}
