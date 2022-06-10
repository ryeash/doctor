package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.ReferenceCounted;
import vest.doctor.http.server.RequestBody;
import vest.doctor.reactive.Rx;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
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
            (a, b) -> a);

    private final ByteBufAllocator alloc;
    private final Flow.Publisher<HttpContent> source;

    public StreamingRequestBody(ChannelHandlerContext ctx, Flow.Publisher<HttpContent> source) {
        this.alloc = ctx.alloc();
        this.source = source;
    }

    @Override
    public Rx<HttpContent> flow() {
        return Rx.from(source).runOnComplete(() -> {
        });
    }

    @Override
    public Flow.Publisher<ByteBuf> asBuffer() {
        return flow()
                .collect(Collector.of(alloc::compositeBuffer,
                        (composite, content) -> composite.addComponent(true, content.content()),
                        (a, b) -> a.addComponent(true, b)))
                .map(o -> o);
    }

    @Override
    public Flow.Publisher<String> asString() {
        return flow()
                .map(c -> {
                    try {
                        return c.content().toString(StandardCharsets.UTF_8);
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
}
