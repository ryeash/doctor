package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class StreamingBody extends InputStream {
    private static Logger log = LoggerFactory.getLogger(StreamingBody.class);

    private CompositeByteBuf composite = Unpooled.compositeBuffer();
    private CompletableFuture<ByteBuf> future = new CompletableFuture<>();
    private final long maxLength;
    private long size;
    private BiConsumer<ByteBuf, Boolean> dataConsumer;
    private HttpHeaders trailingHeaders;

    private boolean inputStreamClosed = false;

    public StreamingBody(long maxLength) {
        this.maxLength = maxLength;
        this.size = 0;
    }

    public CompletableFuture<ByteBuf> future() {
        return future;
    }

    public Optional<HttpHeaders> trailingHeaders() {
        return Optional.ofNullable(trailingHeaders).filter(h -> !h.isEmpty());
    }

    public void readData(BiConsumer<ByteBuf, Boolean> dataConsumer) {
        this.dataConsumer = dataConsumer;
        dataConsumer.accept(composite, future.isDone());
        composite.discardReadComponents();
    }

    public void append(HttpContent content) {
        log.info("append data {}", content);
        composite.addComponent(true, content.content());

        size += content.content().readableBytes();
        if (size >= maxLength) {
            throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
        }

        if (content instanceof LastHttpContent) {
            log.info("last content");
            this.trailingHeaders = ((LastHttpContent) content).trailingHeaders();
            future.complete(composite);
        }
        if (dataConsumer != null) {
            dataConsumer.accept(composite, future.isDone());
            composite.discardReadComponents();
        }
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public int read() {
        readWait();
        if (composite.readableBytes() <= 0) {
            return -1;
        } else {
            try {
                return composite.readByte();
            } finally {
                composite.discardReadComponents();
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        readWait();
        if (composite.readableBytes() <= 0) {
            return -1;
        } else {
            int toRead = Math.min(len, composite.readableBytes());
            composite.readBytes(b, off, toRead);
            composite.discardReadComponents();
            return toRead;
        }
    }

    @Override
    public void close() {
        this.inputStreamClosed = true;
        composite.release();
    }

    private void readWait() {
        if (inputStreamClosed) {
            throw new UncheckedIOException(new IOException("this input has been closed"));
        }
        while (!future.isDone() && composite.readableBytes() <= 0) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }
}
