package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.flow.Flo;

import java.util.function.UnaryOperator;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    /**
     * Get the flow of request body data. A call to this method will mark this object
     * as 'consumed' and any further method calls (except calls to {@link #inspect(UnaryOperator)})
     * will result in {@link IllegalStateException}.
     *
     * @return a flow of request body contents
     */
    Flo<?, HttpContent> flow();

    /**
     * Inspect the data flow. A call to this method will NOT mark this object as consumed. It
     * will, however, check the consumed flag and throw an {@link IllegalStateException} if the
     * data has already been consumed. Care should be taken to avoid altering the content of the
     * flow as that could have undesirable consequences for the performance of the underlying http
     * channels.
     *
     * @param inspection a function that takes the data flow and returns a type compatible flow
     */
    void inspect(UnaryOperator<Flo<?, HttpContent>> inspection);

    /**
     * Collect the body into a single buffer.
     *
     * @return a flow of the request body as a single buffer
     */
    Flo<?, ByteBuf> asBuffer();

    /**
     * Read the body data into a UTF-8 string.
     *
     * @return a flow of the request body as a single string
     */
    Flo<?, String> asString();

    /**
     * Ignore the body data. The returned future will indicate only that the data has been
     * received successfully in its entirety.
     *
     * @return a flow of a single null element indicating the successful read of all body data
     */
    <T> Flo<?, T> ignored();

    /**
     * Get the usability state of the request body. If this method returns true, calling any
     * other method will throw an {@link IllegalStateException}.
     *
     * @return true if the body has already been used
     */
    boolean used();
}
