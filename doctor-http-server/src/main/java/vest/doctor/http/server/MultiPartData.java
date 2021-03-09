package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a multi part upload from the client.
 */
public interface MultiPartData {

    /**
     * Verify if the client sent multipart data.
     */
    boolean valid();

    /**
     * Get a future that will complete when all parts of the multipart upload are received.
     * If {@link #valid()} is false, the future will be completed exceptionally.
     *
     * @return the future parts
     */
    CompletableFuture<Iterable<Part>> future();

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
    }
}
