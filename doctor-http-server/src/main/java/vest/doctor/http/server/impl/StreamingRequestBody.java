package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.RequestBody;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * A handle to the HTTP request body. Supports asynchronously reading the body data.
 */
public class StreamingRequestBody implements RequestBody {
    private final CompositeByteBuf composite;
    private final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
    private final long maxLength;
    private long size;

    private CompletableFuture<Object> asyncReadFuture;
    private BiFunction<ByteBuf, Boolean, ?> dataConsumer;
    private HttpHeaders trailingHeaders;

    private volatile boolean closed = false;

    public StreamingRequestBody(CompositeByteBuf compositeByteBuf, long maxLength) {
        this.composite = compositeByteBuf;
        this.maxLength = maxLength;
        this.size = 0;
    }

    @Override
    public CompletableFuture<ByteBuf> completionFuture() {
        return future;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> asyncRead(BiFunction<ByteBuf, Boolean, T> reader) {
        if (this.dataConsumer != null) {
            throw new IllegalStateException("there is already a data consumer attached to this body");
        }
        this.asyncReadFuture = new CompletableFuture<>();
        asyncReadFuture.whenCompleteAsync((v, err) -> composite.release());
        this.dataConsumer = reader;
        internalAsyncRead();
        return (CompletableFuture<T>) asyncReadFuture;
    }

    private void internalAsyncRead() {
        if (dataConsumer == null) {
            return;
        }
        synchronized (composite) {
            try {
                Object result = dataConsumer.apply(composite, future.isDone());
                if (composite.refCnt() > 0) {
                    composite.discardReadComponents();
                }
                if (result != null) {
                    asyncReadFuture.complete(result);
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
                if (asyncReadFuture != null) {
                    asyncReadFuture.completeExceptionally(t);
                }
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
                closed = true;
                composite.release();
                throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
            if (readable > 0) {
                synchronized (composite) {
                    composite.addComponent(true, buf);
                    size += readable;
                    composite.notifyAll();
                }
            }

            if (content instanceof LastHttpContent last) {
                this.trailingHeaders = last.trailingHeaders();
                future.complete(composite);
            }
            internalAsyncRead();
        } finally {
            synchronized (composite) {
                composite.notifyAll();
            }
        }
    }

    @Override
    public String toString() {
        return "StreamingBody{" + composite + ", done?:" + future.isDone() + "}";
    }
}
