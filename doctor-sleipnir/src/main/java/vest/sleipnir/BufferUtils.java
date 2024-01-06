package vest.sleipnir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferUtils {
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
}
