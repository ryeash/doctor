package vest.doctor.sleipnir;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferUtils {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    public static final ByteBuffer CLOSE_BUFFER = ByteBuffer.allocate(0);

    public static ByteBuffer copy(ByteBuffer bb) {
        ByteBuffer copy = ByteBuffer.allocate(bb.remaining());
        transfer(bb, copy);
        copy.flip();
        return copy;
    }

    public static void transfer(ByteBuffer src, ByteBuffer dst) {
        int toTx = Math.min(src.remaining(), dst.remaining());
        dst.put(src.slice(src.position(), toTx));
        src.position(src.position() + toTx);
    }

    public static String toString(ByteBuffer bb) {
        bb.mark();
        try {
            return StandardCharsets.UTF_8.decode(bb).toString();
        } finally {
            bb.reset();
        }
    }

    public static InputStream toInputStream(ByteBuffer bb) {
        return new InputStream() {
            @Override
            public int read() {
                if (bb.hasRemaining()) {
                    return bb.get() & 0xFF;
                } else {
                    return -1;
                }
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (!bb.hasRemaining()) {
                    return -1;
                } else {
                    int toRead = Math.min(len, bb.remaining());
                    bb.get(b, off, toRead);
                    return toRead;
                }
            }
        };
    }
}
