package demo.app.service;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Endpoint;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpMethod.GET;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;

import java.util.concurrent.Flow;

@Singleton
@Endpoint("/hello")
@GET
public class TCDirectHandler implements Handler {
    @Override
    public Flow.Publisher<Response> handle(RequestContext context) {
        return Rx.from(context.request().body().ignored())
                .map(context::response)
                .map(r -> r.body(ResponseBody.of("Hello World!")));
    }
}
