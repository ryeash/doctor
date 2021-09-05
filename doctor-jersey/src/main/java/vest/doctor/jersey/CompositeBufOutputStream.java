package vest.doctor.jersey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class CompositeBufOutputStream extends OutputStream implements ChunkedInput<ByteBuf> {

    private static final ByteBuf LAST = Unpooled.wrappedBuffer(new byte[0]);
    private final BlockingQueue<ByteBuf> queue = new LinkedBlockingQueue<>();

    @Override
    public boolean isEndOfInput() {
        return queue.peek() == LAST;
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) {
        return readChunk(ctx.alloc());
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) {
        if (queue.peek() == LAST) {
            return null;
        }
        synchronized (queue) {
            if (queue.peek() == null) {
                return null;
            } else {
                return queue.poll();
            }
        }
    }

    @Override
    public long length() {
        return -1L;
    }

    @Override
    public long progress() {
        return -1L;
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        queue.add(Unpooled.copiedBuffer(b, off, len));
    }

    @Override
    public void write(int b) {
        write(new byte[]{(byte) b});
    }

    @Override
    public void close() throws IOException {
        queue.add(LAST);
    }

    public void teardown() {
        queue.clear();
    }
}
