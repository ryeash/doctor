package vest.doctor.http.server;

import vest.doctor.flow.Flo;
import vest.doctor.tuple.Tuple;

/**
 * Synchronous version of the {@link Handler} interface.
 */
@FunctionalInterface
public interface SynchronousHandler extends Handler {

    @Override
    default Flo<?, Response> handle(Request request) {
        return request.body()
                .asBuffer()
                .map2(buffer -> {
                    try {
                        byte[] buf = new byte[buffer.readableBytes()];
                        buffer.readBytes(buf);
                        return Tuple.of(request, buf);
                    } finally {
                        buffer.release();
                    }
                })
                .map(this::handleSync);
    }

    /**
     * Handle the request synchronously. Using the {@link RequestBody} from the request will
     * cause an {@link IllegalStateException} as the body has already been read into the
     * given byte array.
     *
     * @param request the request
     * @param body    the bytes received in the request body
     * @return a {@link Response}
     */
    Response handleSync(Request request, byte[] body);
}
