package vest.doctor.http.server.impl;

import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class NotFound implements Handler {

    @Override
    public CompletionStage<Response> handle(Request request) {
        return request.body()
                .ignored()
                .thenCombine(CompletableFuture.supplyAsync(request::createResponse),
                        (v, resp) -> resp.status(404).body(EmptyBody.INSTANCE));
    }
}
