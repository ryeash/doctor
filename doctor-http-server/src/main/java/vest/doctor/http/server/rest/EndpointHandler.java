package vest.doctor.http.server.rest;

import vest.doctor.http.server.Request;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface EndpointHandler<P> {
    CompletionStage<Object> handle(P endpoint, Request request, CompletableFuture<?> body) throws Exception;
}
