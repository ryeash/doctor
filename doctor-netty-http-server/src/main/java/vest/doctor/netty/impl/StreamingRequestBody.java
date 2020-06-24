package vest.doctor.netty.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.netty.HttpException;
import vest.doctor.netty.RequestBody;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * A handle to the HTTP request body. Supports event driven reading of body data.
 */
public class StreamingRequestBody extends InputStream implements RequestBody {
    private final CompositeByteBuf composite = Unpooled.compositeBuffer(1024);
    private final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
    private final long maxLength;
    private long size;

    private CompletableFuture<Object> asyncReadFuture;
    private BiFunction<ByteBuf, Boolean, ?> dataConsumer;
    private HttpHeaders trailingHeaders;

    private boolean closed = false;

    public StreamingRequestBody(long maxLength) {
        this.maxLength = maxLength;
        this.size = 0;
    }

    @Override
    public CompletableFuture<ByteBuf> completionFuture() {
        return future;
    }

    @Override
    public InputStream inputStream() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> asyncRead(BiFunction<ByteBuf, Boolean, T> reader) {
        if (this.dataConsumer != null) {
            throw new IllegalStateException("there is already a data consumer attached to this body");
        }
        this.asyncReadFuture = new CompletableFuture<>();
        this.dataConsumer = reader;
        internalAsyncRead();
        return (CompletableFuture<T>) asyncReadFuture;
    }

    private void internalAsyncRead() {
        if (dataConsumer == null) {
            return;
        }
        synchronized (composite) {
            Object result = dataConsumer.apply(composite, future.isDone());
            if (composite.refCnt() > 0) {
                composite.discardReadComponents();
            }
            if (result != null) {
                asyncReadFuture.complete(result);
            }
        }
    }

    /**
     * Get the trailing headers attached to the last content. If the body has not been fully read, or there
     * were no trailing headers, the returned optional will be empty.
     *
     * @return the optional trailing headers
     */
    public Optional<HttpHeaders> trailingHeaders() {
        return Optional.ofNullable(trailingHeaders).filter(h -> !h.isEmpty());
    }

    public void append(HttpContent content) {
        try {
            if (closed) {
                content.release();
                return;
            }
            ByteBuf buf = content.content();
            int readable = buf.readableBytes();
            if (size + readable >= maxLength) {
                throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            if (readable > 0) {
                synchronized (composite) {
                    composite.addComponent(true, buf);
                    size += readable;
                }
            }

            if (content instanceof LastHttpContent) {
                this.trailingHeaders = ((LastHttpContent) content).trailingHeaders();
                future.complete(composite);
            }
            internalAsyncRead();
        } finally {
            synchronized (this) {
                notifyAll();
            }
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
    public int available() {
        return composite.readableBytes();
    }

    @Override
    public void close() {
        this.closed = true;
        composite.release();
    }

    @Override
    public String toString() {
        return "StreamingBody{" + composite + ", done?:" + future.isDone() + "}";
    }

    private void readWait() {
        if (closed) {
            return;
        }
        while (!future.isDone() && composite.readableBytes() <= 0) {
            synchronized (composite) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }
}
