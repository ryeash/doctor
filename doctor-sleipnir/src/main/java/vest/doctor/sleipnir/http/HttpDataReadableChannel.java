package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class HttpDataReadableChannel implements ReadableByteChannel {
    private final HttpData httpData;
    private ByteBuffer byteBuffer;

    public HttpDataReadableChannel(HttpData httpData) {
        this.httpData = httpData;
    }

    @Override
    public int read(ByteBuffer dst) {
        if (byteBuffer == null) {
            byteBuffer = httpData.serialize();
        }
        return BufferUtils.transfer(byteBuffer, dst);
    }

    @Override
    public boolean isOpen() {
        if (byteBuffer == null) {
            byteBuffer = httpData.serialize();
        }
        return byteBuffer.hasRemaining();
    }

    @Override
    public void close() {
    }
}
