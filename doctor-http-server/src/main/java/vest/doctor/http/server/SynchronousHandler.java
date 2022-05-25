package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import vest.doctor.reactive.Flo;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {
    @Override
    default Flo<?, Response> handle(RequestContext requestContext) throws Exception {
        return requestContext.request()
                .body()
                .asBuffer()
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
