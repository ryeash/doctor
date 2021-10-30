package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.workflow.Workflow;

final class NotFound implements Handler {

    @Override
    public Workflow<?, Response> handle(Request request) {
        return request.body()
                .ignored()
                .map(v -> {
                    return request.createResponse()
                            .status(HttpResponseStatus.NOT_FOUND)
                            .body(EmptyBody.INSTANCE);
                });
    }
}
