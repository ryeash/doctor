package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

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

    /**
     * Represents a single part of a multipart upload.
     */
    interface Part {

        /**
         * The type of the part, one of: FileUpload, Attribute, InternalAttribute, or empty if this is the final
         * part.
         */
        String type();

        /**
         * The name of the part. Will be empty when <code>last() == true</code>.
         */
        String name();

        /**
         * Get the byte data for the part.
         */
        ByteBuf data();

        /**
         * If this is the last part that will be received. When true, no further
         * Parts will be read, and type, name and data will all be empty.
         */
        boolean last();
    }
}
