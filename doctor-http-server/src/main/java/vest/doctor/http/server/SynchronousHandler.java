package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import vest.doctor.flow.Flo;

/**
 * Synchronous version of the {@link Handler} interface.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default Flo<?, Response> handle(Request request) {
        return request.body()
                .asBuffer()
                .map(buffer -> handleSync(request, buffer));
    }

    /**
     * Handle the request synchronously.
     *
     * @param request the request
     * @return a {@link Response}
     */
    Response handleSync(Request request, ByteBuf body);
}
