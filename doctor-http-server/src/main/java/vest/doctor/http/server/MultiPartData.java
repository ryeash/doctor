package vest.doctor.http.server;

import java.util.concurrent.Flow;

/**
 * Represents a multipart upload from the client.
 */
public interface MultiPartData {

    /**
     * Verify if the client sent conforming multipart data.
     */
    boolean valid();

    /**
     * Get the parts of the multipart request body as an asynchronous flow.
     */
    Flow.Publisher<Part> parts();

}
