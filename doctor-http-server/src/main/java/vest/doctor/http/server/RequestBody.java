package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;

import java.util.concurrent.Flow;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    /**
     * Get the flow of request body data. A call to this method will mark this object
     * as 'consumed' and any further method calls.
     *
     * @return a flow of request body contents
     */
    Flow.Publisher<HttpContent> flow();

    /**
     * Collect the body into a single buffer.
     *
     * @return a flow of the request body as a single buffer
     */
    Flow.Publisher<ByteBuf> asBuffer();

    /**
     * Read and collect the body data into a single UTF-8 string.
     *
     * @return a flow of the request body as a single string
     */
    Flow.Publisher<String> asString();

    /**
     * Ignore the body data.
     *
     * @return a flow of a single null element indicating the successful read of all body data
     */
    <T> Flow.Publisher<T> ignored();
}
