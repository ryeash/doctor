package vest.doctor.http.server;

import vest.doctor.flow.Flo;
import vest.doctor.function.ThrowingBiFunction;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface Handler {

    /**
     * Handle a request and produce a response.
     *
     * @param request the request
     * @return an asynchronously produced response flow
     */
    Flo<?, Response> handle(Request request) throws Exception;

    /**
     * Create a handler from a synchronous function accepting the request and the body as a
     * byte array.
     *
     * @param function the synchronous request handler
     * @return a new Handler
     */
    static Handler sync(ThrowingBiFunction<Request, byte[], Response> function) {
        return request -> request.body()
                .asBuffer()
                .chain((buffer, sub, emitter) -> {
                    byte[] buf = new byte[buffer.readableBytes()];
                    buffer.readBytes(buf);
                    buffer.release();
                    emitter.emit(function.applyThrows(request, buf));
                });
    }

    /**
     * Container record holding a handler and a summary string of the handler to enrich, e.g., the
     * method reference used as a handler with a more descriptive string.
     */
    record Holder(Handler handler, String summary) implements Handler {
        @Override
        public Flo<?, Response> handle(Request request) throws Exception {
            return handler.handle(request);
        }

        @Override
        public String toString() {
            return summary;
        }
    }
}
