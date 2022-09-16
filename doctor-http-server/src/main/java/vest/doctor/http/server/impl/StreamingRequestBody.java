package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.ReferenceCounted;
import vest.doctor.http.server.RequestBody;
import vest.doctor.reactive.Rx;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A handle to the HTTP request body. Supports asynchronously reading the body data.
 */
public class StreamingRequestBody implements RequestBody {
    public static final Collector<Object, ?, Object> IGNORING_COLLECTOR = Collector.of(
            Object::new,
            (a, b) -> {
            },
            (a, b) -> null);

    private final ByteBufAllocator alloc;
    private final Flow.Publisher<HttpContent> source;

    public StreamingRequestBody(ChannelHandlerContext ctx, Flow.Publisher<HttpContent> source) {
        this.alloc = ctx.alloc();
        this.source = source;
    }

    @Override
    public Rx<HttpContent> flow() {
        // TODO: find a better way to not lose data
        // this NO_OP is needed because without the initial subscriber to SubmissionPublisher, we lose data
        return Rx.from(source).runOnComplete(Rx.NO_OP);
    }

    @Override
    public Flow.Publisher<ByteBuf> asBuffer() {
        CompositeByteBuf comp = alloc.compositeBuffer();
        return flow()
                .collect(Collector.of(() -> comp,
                        (composite, content) -> composite.addComponent(true, content.content()),
                        (a, b) -> a.addComponent(true, b),
                        Function.identity(),
                        Collector.Characteristics.IDENTITY_FINISH));
    }

    @Override
    public Flow.Publisher<String> asString() {
        return asString(StandardCharsets.UTF_8);
    }

    @Override
    public Flow.Publisher<String> asString(Charset charset) {
        return flow()
                .map(c -> {
                    try {
                        return c.content().toString(charset);
                    } finally {
                        c.release();
                    }
                })
                .collect(Collectors.joining());
    }

    @Override
    public <T> Flow.Publisher<T> ignored() {
        return flow()
                .map(ReferenceCounted::release)
                .collect(IGNORING_COLLECTOR)
                .map(c -> null);
    }

    @Override
    public Flow.Publisher<byte[]> chunked() {
        return flow()
                .map(content -> {
                    try {
                        byte[] bytes = new byte[content.content().readableBytes()];
                        content.content().readBytes(bytes);
                        return bytes;
                    } finally {
                        content.release();
                    }
                });
    }
}
