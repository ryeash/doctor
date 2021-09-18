package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    /**
     * Attach a function to this body that will receive data chunks as they arrive.
     * Only one call to this method is allowed; calling more than once will result
     * in an {@link IllegalStateException}
     *
     * @param reader the function that will process the data. The function
     *               must return <code>null</code> until a complete body has been read
     * @return a future representing the completed parsing of request body data with
     * the non-null output from the reader function
     */
    <T> CompletableFuture<T> asyncRead(Function<HttpContent, T> reader);

    /**
     * Get a future that will complete when the final bytes are received from the client.
     */
    CompletableFuture<ByteBuf> completionFuture();

    /**
     * Read the body data into a UTF-8 string.
     */
    CompletableFuture<String> asString();

    /**
     * Ignore the body data. The returned future will indicate only that the data has been
     * received successfully in its entirety.
     */
    CompletableFuture<Void> ignored();

    /**
     * Return true if the asyncRead method has already been called. If this method returns true,
     * additional calls to any method besides this one with throw an exception.
     */
    boolean readerAttached();
}
