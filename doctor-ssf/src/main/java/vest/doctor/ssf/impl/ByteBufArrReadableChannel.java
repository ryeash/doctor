package vest.doctor.ssf.impl;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteBufArrReadableChannel implements ReadableByteChannel {
    private final ByteBuffer[] arr;
    private int i = 0;

    public ByteBufArrReadableChannel(ByteBuffer[] arr) {
        this.arr = arr;
    }

    @Override
    public int read(ByteBuffer dst) {
        if (!isOpen()) {
            return -1;
        }
        int start = dst.position();
        while (i < arr.length && dst.hasRemaining()) {
            ByteBuffer src = arr[i];
            if (!src.hasRemaining()) {
                i++;
                continue;
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
        }
        return dst.position() - start;
    }

    @Override
    public boolean isOpen() {
        return i < arr.length;
    }

    @Override
    public void close() {
        i = Integer.MAX_VALUE;
    }
}
