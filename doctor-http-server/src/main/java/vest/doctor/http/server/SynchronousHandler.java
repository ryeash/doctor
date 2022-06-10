package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import vest.doctor.reactive.Rx;

import java.util.concurrent.Flow;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {
    @Override
    default Flow.Publisher<Response> handle(RequestContext requestContext) throws Exception {
        return Rx.from(requestContext.request()
                        .body()
                        .asBuffer())
                .map(body -> {
                    try {
                        return handleSync(requestContext, body);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    Response handleSync(RequestContext requestContext, ByteBuf requestBody) throws Exception;
}
