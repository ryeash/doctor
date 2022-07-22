package demo.app.service;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.FilterChain;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;

import java.util.Objects;
import java.util.concurrent.Flow;

@Singleton
public class TestFilter implements Filter {
    @Override
    public Flow.Publisher<Response> filter(RequestContext requestContext, FilterChain chain) throws Exception {
        Request request = requestContext.request();
        if (Objects.equals(request.queryParam("halt"), "true")) {
            return Rx.from(request.body().ignored())
                    .map(requestContext::response)
                    .map(response -> response.status(202).body(ResponseBody.of("halted")));
        }

        requestContext.attribute("test.attribute", "value");
        return Rx.from(chain.next(requestContext))
                .map(r -> r.header("X-After", "true"));
    }
}
