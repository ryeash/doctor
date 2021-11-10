package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.flow.Flo;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    Flo<?, HttpContent> flow();

    /**
     * Get a future that will complete when the final bytes are received from the client.
     */
    Flo<?, ByteBuf> asBuffer();

    /**
     * Read the body data into a UTF-8 string.
     */
    Flo<?, String> asString();

    /**
     * Ignore the body data. The returned future will indicate only that the data has been
     * received successfully in its entirety.
     */
    <T> Flo<?, T> ignored();

    boolean used();
}
