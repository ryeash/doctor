package demo.app.service;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Endpoint;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpMethod.GET;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.ResponseBody;

import java.util.concurrent.CompletableFuture;

@Singleton
@Endpoint("/hello")
@GET
public class TCDirectHandler implements Handler {
    @Override
    public CompletableFuture<RequestContext> handle(RequestContext context) {
        context.response().body(ResponseBody.of("Hello World!"));
        return CompletableFuture.completedFuture(context);
    }
}
