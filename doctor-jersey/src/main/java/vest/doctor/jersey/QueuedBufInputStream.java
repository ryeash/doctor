package vest.doctor.jersey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

final class QueuedBufInputStream extends InputStream {

    private static final int MAX_READ_DELAY = 500;
    private static final ByteBuf LAST = Unpooled.EMPTY_BUFFER;
    private final BlockingQueue<ByteBuf> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final byte[] oneByte = new byte[1];

    @Override
    public int read() {
        int read = read(oneByte, 0, 1);
        if (read >= 0) {
            return oneByte[0];
        } else {
            return read;
        }
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        waitForData();
        synchronized (queue) {
            ByteBuf top = queue.peek();
            if (top == null) {
                return 0;
            } else if (top == LAST) {
                return -1;
            } else {
                int toRead = Math.min(top.readableBytes(), len);
                top.readBytes(b, off, toRead);
                if (!top.isReadable()) {
                    queue.poll();
                    top.release();
                }
                return toRead;
            }
        }
    }

    @Override
    public void close() {
    }

    public void reset() {
        ByteBuf b;
        while ((b = queue.poll()) != null) {
            b.release();
        }
        queue.clear();
        size.set(0);
    }

    public int size() {
        return size.get();
    }

    public void append(HttpContent next) {
        synchronized (queue) {
            boolean added = false;
            if (next != null && next.content().isReadable()) {
                size.addAndGet(next.content().readableBytes());
                added = queue.add(next.content());
            }
            if (next instanceof LastHttpContent) {
                added = queue.add(LAST);
            }
            if (added) {
                queue.notifyAll();
            }
        }
    }

    private void waitForData() {
        if (queue.isEmpty()) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait(MAX_READ_DELAY);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
            }
        }
    }
}
