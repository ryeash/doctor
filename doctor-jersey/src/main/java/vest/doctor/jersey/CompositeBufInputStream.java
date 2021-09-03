package vest.doctor.jersey;

import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompositeBufInputStream extends InputStream {

    private final CompositeByteBuf buf;
    private final AtomicBoolean lastContent = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public CompositeBufInputStream(CompositeByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eof()) {
            return -1;
        }
        int toRead = Math.min(buf.readableBytes(), len);
        buf.readBytes(b, off, toRead);
        return toRead;
    }

    @Override
    public int read() throws IOException {
        if (eof()) {
            return -1;
        } else {
            return buf.readByte();
        }
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        buf.clear().release();
    }

    public void append(HttpContent next) {
        if (next instanceof LastHttpContent) {
            lastContent.set(true);
        }
        buf.addComponent(true, next.content());
    }

    private boolean eof() {
        return lastContent.get() && !buf.isReadable();
    }
}
