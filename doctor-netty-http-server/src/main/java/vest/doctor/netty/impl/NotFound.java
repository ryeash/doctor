package vest.doctor.netty.impl;

import vest.doctor.netty.Handler;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;

import java.util.concurrent.CompletableFuture;

public class NotFound implements Handler {

    @Override
    public CompletableFuture<Response> handle(Request request) {
        return request.body()
                .ignored()
                .thenCombine(CompletableFuture.supplyAsync(request::createResponse),
                        (v, resp) -> resp.status(404));
    }
}
