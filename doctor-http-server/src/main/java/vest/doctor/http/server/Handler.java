package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.http.server.impl.HttpException;
import vest.doctor.reactive.Rx;

import java.util.concurrent.Flow;
import java.util.function.BiFunction;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface Handler {

    /**
     * Process an HTTP request.
     *
     * @param requestContext the request context
     * @return an asynchronous Response result
     * @throws Exception for any error during processing
     */
    Flow.Publisher<Response> handle(RequestContext requestContext) throws Exception;

    /**
     * Create a request Handler with a synchronous function. The request body will be buffered
     * into a single {@link ByteBuf} and automatically {@link ByteBuf#release() released} after the function is called.
     *
     * @param function the handler function that returns a {@link Response}
     * @return a new handler
     */
    static Handler sync(BiFunction<RequestContext, ByteBuf, Response> function) {
        return requestContext -> Rx.from(requestContext.request().body().asBuffer())
                .map(body -> {
                    try {
                        return function.apply(requestContext, body);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new HttpException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
                    } finally {
                        body.release();
                    }
                });
    }
}
