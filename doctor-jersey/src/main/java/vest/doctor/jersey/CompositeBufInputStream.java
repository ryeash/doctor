package vest.doctor.jersey;

import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class CompositeBufInputStream extends InputStream {

    private final CompositeByteBuf buf;
    private final AtomicBoolean lastContent = new AtomicBoolean(false);

    public CompositeBufInputStream(CompositeByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        synchronized (buf) {
            if (eof()) {
                return -1;
            } else {
                int toRead = Math.min(buf.readableBytes(), len);
                if (toRead > 0) {
                    buf.readBytes(b, off, toRead);
                    if (buf.refCnt() > 0) {
                        buf.discardReadComponents();
                    }
                }
                return toRead;
            }
        }
    }

    @Override
    public int read() {
        synchronized (buf) {
            if (eof()) {
                return -1;
            } else {
                return buf.readByte();
            }
        }
    }

    @Override
    public void close() {
    }

    public void reset() {
        synchronized (buf) {
            buf.clear();
            lastContent.set(false);
        }
    }

    public void teardown() {
        buf.release();
    }

    public int size() {
        return buf.writerIndex();
    }

    public void append(HttpContent next) {
        synchronized (buf) {
            if (next instanceof LastHttpContent) {
                lastContent.set(true);
            }
            if (next.content().isReadable()) {
                buf.addFlattenedComponents(true, next.content());
            }
        }
    }

    private boolean eof() {
        if (!lastContent.get()) {
            return false;
        }
        return !buf.isReadable();
    }
}
