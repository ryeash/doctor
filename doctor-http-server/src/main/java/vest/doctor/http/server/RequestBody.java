package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.workflow.Workflow;

/**
 * A handle to the request body data. The data is received asynchronously.
 */
public interface RequestBody {

    Workflow<?, HttpContent> flow();

    /**
     * Get a future that will complete when the final bytes are received from the client.
     */
    Workflow<?, ByteBuf> asBuffer();

    /**
     * Read the body data into a UTF-8 string.
     */
    Workflow<?, String> asString();

    /**
     * Ignore the body data. The returned future will indicate only that the data has been
     * received successfully in its entirety.
     */
    Workflow<?, Void> ignored();

    boolean used();
}
