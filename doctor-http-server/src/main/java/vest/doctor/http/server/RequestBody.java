package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    /**
     * Get a future that will complete when the final bytes are received from the client.
     */
    CompletableFuture<ByteBuf> completionFuture();

    /**
     * Attach a function to this body that will receive data chunks as they arrive.
     * Only one call to this method is allowed; calling more than once will result
     * in an {@link IllegalStateException}
     *
     * @param reader the function that will process the data, the boolean value will
     *               indicate if the request body has been fully read. The function
     *               must return <code>null</code> until a complete body has been read
     * @return a future representing the completed parsing of request body data with
     * the non-null output from the reader function
     */
    <T> CompletableFuture<T> asyncRead(BiFunction<ByteBuf, Boolean, T> reader);

    /**
     * Get any trailing headers that were sent after the end of the request body stream.
     * The returned optional will always be empty if the end of the body has not been
     * read.
     */
    Optional<HttpHeaders> trailingHeaders();

    /**
     * Read the body data into a UTF-8 string.
     */
    default CompletableFuture<String> asString() {
        return asyncRead((buf, finished) -> {
            if (finished) {
                try {
                    return buf.toString(StandardCharsets.UTF_8);
                } finally {
                    buf.readerIndex(buf.writerIndex());
                    buf.release();
                }
            } else {
                return null;
            }
        });
    }

    /**
     * Ignore the body data. The returned future will indicate only that the data has been
     * received successfully in its entirety.
     */
    default CompletableFuture<Boolean> ignored() {
        return asyncRead((buf, finished) -> {
            buf.readerIndex(buf.writerIndex());
            return finished ? true : null;
        });
    }
}
