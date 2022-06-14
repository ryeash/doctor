package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;

import java.util.concurrent.Flow;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    /**
     * Get the {@link Flow.Publisher} of request body data.
     *
     * @return a publisher of request body contents
     */
    Flow.Publisher<HttpContent> flow();

    /**
     * Collect the body into a single buffer.
     *
     * @return a publisher of the request body as a single buffer
     */
    Flow.Publisher<ByteBuf> asBuffer();

    /**
     * Collect the body data into a single UTF-8 string.
     *
     * @return a publisher of the request body as a single string
     */
    Flow.Publisher<String> asString();

    /**
     * Ignore the body data.
     *
     * @return a publisher of a single null element indicating the successful read of all body data
     */
    <T> Flow.Publisher<T> ignored();
}
