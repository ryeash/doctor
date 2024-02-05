package vest.doctor.sleipnir;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ByteBufferReadableByteChannel implements ReadableByteChannel {

    private final ByteBuffer byteBuffer;

    public ByteBufferReadableByteChannel(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int read(ByteBuffer dst) {
        if (byteBuffer.hasRemaining()) {
            return BufferUtils.transfer(byteBuffer, dst);
        } else {
            return -1;
        }
    }

    @Override
    public boolean isOpen() {
        return byteBuffer.hasRemaining();
    }

    @Override
    public void close() {
    }
}
