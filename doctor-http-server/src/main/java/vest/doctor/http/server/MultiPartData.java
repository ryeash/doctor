package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

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
     * Register a listener to consumer parts as they arrive.
     *
     * @param consumer the action to take on the part
     * @return a future indicating when all the part data has been received
     */
    CompletableFuture<Boolean> receive(Consumer<Part> consumer);

    /**
     * Represents a single part of a multipart upload.
     */
    interface Part {

        /**
         * The type of the part, one of: FileUpload, Attribute, or InternalAttribute.
         */
        String getType();

        /**
         * The name of the part.
         */
        String getName();

        /**
         * Get the byte data for the part.
         */
        ByteBuf getData();

        /**
         * If this is the last part that will be received. When true, no further
         * Parts will be read, and type, name and data will all be empty.
         */
        boolean isLast();
    }
}
