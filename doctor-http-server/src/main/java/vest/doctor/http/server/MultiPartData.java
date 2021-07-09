package vest.doctor.http.server;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a multi part upload from the client.
 */
public interface MultiPartData {

    /**
     * Verify if the client sent multipart data.
     */
    boolean valid();

    /**
     * Register a listener to consume {@link Part}s as they arrive.
     *
     * @param consumer the action
     * @return a future indicating when all parts have been received
     */
    CompletableFuture<Boolean> receive(Consumer<Part> consumer);

}
