package vest.doctor.ssf.impl;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteBufReadableChannel implements ReadableByteChannel {
    private final ByteBuffer src;

    public ByteBufReadableChannel(ByteBuffer src) {
        this.src = src;
    }

    @Override
    public int read(ByteBuffer dst) {
        if (!src.hasRemaining()) {
            return -1;
        }
        int toTX = Math.min(src.remaining(), dst.remaining());
        if (src.hasArray()) {
            dst.put(src.array(), src.position() + src.arrayOffset(), toTX);
            src.position(src.position() + toTX);
        } else {
            for (int i = 0; i < toTX; i++) {
                dst.put(src.get());
            }
        }
        return toTX;
    }

    @Override
    public boolean isOpen() {
        return src.hasRemaining();
    }

    @Override
    public void close() {
        src.position(src.limit());
    }
}
