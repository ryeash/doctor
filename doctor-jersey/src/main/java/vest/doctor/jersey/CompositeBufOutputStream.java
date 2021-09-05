package vest.doctor.jersey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class CompositeBufOutputStream extends OutputStream implements ChunkedInput<ByteBuf> {

    private final CompositeByteBuf buf;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public CompositeBufOutputStream(CompositeByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public boolean isEndOfInput() {
        synchronized (buf) {
            return closed.get() && !buf.isReadable();
        }
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) {
        return readChunk(ctx.alloc());
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) {
        return Unpooled.unreleasableBuffer(buf);
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public long progress() {
        return buf.readerIndex();
    }

    @Override
    public void write(byte[] b, int off, int len) {
        synchronized (buf) {
            buf.addComponent(true, Unpooled.copiedBuffer(b, off, len));
            buf.discardReadComponents();
        }
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
    }

    public void teardown() {
        synchronized (buf) {
            buf.release();
        }
    }
}
