package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Singleton
public class TCNettyFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(TCNettyFilter.class);

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
