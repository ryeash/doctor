package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import vest.doctor.flow.Flo;

/**
 * Represents a multi-part upload from the client.
 */
public interface MultiPartData {

    /**
     * Verify if the client sent multipart data.
     */
    boolean valid();

    /**
     * Get the parts of the multipart request body as an asynchronous flow.
     */
    Flo<?, Part> parts();

    /**
     * Represents a single part of a multipart upload.
     */
    interface Part {

        /**
         * The type of the part, one of: FileUpload, Attribute, or InternalAttribute.
         */
        String type();

        /**
         * The name of the part.
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
