package vest.doctor.sleipnir;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public class BufferUtils {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    public static final ReadableByteChannel EMPTY_CHANNEL = new ReadableByteChannel() {
        @Override
        public int read(ByteBuffer dst) {
            return -1;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
        }
    };
    public static final ReadableByteChannel CLOSE_CHANNEL = new ReadableByteChannel() {
        @Override
        public int read(ByteBuffer dst) {
            return 0;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
        }
    };

    public static ByteBuffer copy(ByteBuffer bb) {
        ByteBuffer copy = ByteBuffer.allocate(bb.remaining());
        transfer(bb, copy);
        copy.flip();
        return copy;
    }

    public static int transfer(ByteBuffer src, ByteBuffer dst) {
        int toTx = Math.min(src.remaining(), dst.remaining());
        dst.put(src.slice(src.position(), toTx));
        src.position(src.position() + toTx);
        return toTx;
    }

    public static int indexOf(ByteBuffer src, byte... stop) {
        int position = src.position();
        for (int i = position; i < src.remaining() - stop.length + 1; i++) {
            if (src.get(i) == stop[0]) {
                boolean fullMatch = true;
                for (int j = 0; j < stop.length; j++) {
                    fullMatch = src.get(i + j) == stop[j];
                    if (!fullMatch) {
                        break;
                    }
                }
                if (fullMatch) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int indexOf(ByteBuffer buf, byte b, int from) {
        int pos = buf.position();
        for (int i = from; i < buf.remaining(); i++) {
            if (buf.get(pos + i) == b) {
                return i;
            }
        }
        return -1;
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
