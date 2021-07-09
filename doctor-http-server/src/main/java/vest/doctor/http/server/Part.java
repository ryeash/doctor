package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

/**
 * Represents a single part of a multipart upload.
 */
public interface Part {

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
