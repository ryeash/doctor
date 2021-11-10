package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

final class NotFound implements Handler {

    @Override
    public Flo<?, Response> handle(Request request) {
        return request.body()
                .ignored()
                .map(request::createResponse)
                .observe(r -> r.status(HttpResponseStatus.NOT_FOUND)
                        .body(EmptyBody.INSTANCE));
    }
}
