package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

final class NotFound implements Handler {
    static final Handler INSTANCE = new NotFound();

    @Override
    public Flo<?, Response> handle(Request request) {
        return request.body()
                .ignored()
                .map(request::createResponse)
                .map(r -> r.status(HttpResponseStatus.NOT_FOUND)
                        .body(EmptyBody.INSTANCE));
    }
}
