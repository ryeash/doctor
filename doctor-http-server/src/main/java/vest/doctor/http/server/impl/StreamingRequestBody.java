package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCounted;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.RequestBody;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * A handle to the HTTP request body. Supports asynchronously reading the body data.
 */
public class StreamingRequestBody implements RequestBody {
    private final ByteBufAllocator alloc;
    private final long maxLength;
    private final AtomicLong size;

    private volatile CompletableFuture<Object> future;
    private Function<HttpContent, ?> reader;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public StreamingRequestBody(ChannelHandlerContext ctx, long maxLength) {
        this.alloc = ctx.alloc();
        this.maxLength = maxLength;
        this.size = new AtomicLong(0);
    }

    @Override
    public CompletableFuture<ByteBuf> completionFuture() {
        CompositeByteBuf buf = alloc.compositeBuffer(128);
        return asyncRead(content -> {
            if (content.content().isReadable()) {
                buf.addComponent(true, content.content());
            }
            return buf;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> asyncRead(Function<HttpContent, T> reader) {
        if (this.future != null) {
            throw new IllegalStateException("body is already being consumed");
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        this.future = (CompletableFuture<Object>) future;
        this.reader = reader;
        return future;
    }

    public void append(HttpContent content) {
        try {
            synchronized (finished) {
                if (finished.get()) {
                    throw new IllegalStateException("no more content expected for request");
                }
                if (size.addAndGet(content.content().readableBytes()) >= maxLength) {
                    throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
                }
                if (reader != null) {
                    Object apply = reader.apply(content);
                    if (content instanceof LastHttpContent) {
                        finished.set(true);
                        future.complete(apply);
                    }
                }
            }
        } catch (Throwable t) {
            reader = null;
            future.completeExceptionally(t);
        }
    }

    @Override
    public CompletableFuture<String> asString() {
        return completionFuture()
                .thenApply(buf -> {
                    try {
                        return buf.toString(StandardCharsets.UTF_8);
                    } finally {
                        buf.release();
                    }
                });
    }

    @Override
    public CompletableFuture<Void> ignored() {
        return asyncRead(ReferenceCounted::release).thenApply(b -> null);
    }

    @Override
    public boolean readerAttached() {
        return future != null;
    }
}
