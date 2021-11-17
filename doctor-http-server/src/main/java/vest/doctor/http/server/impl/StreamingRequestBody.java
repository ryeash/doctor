package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.RequestBody;

import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A handle to the HTTP request body. Supports asynchronously reading the body data.
 */
public class StreamingRequestBody implements RequestBody {
    private static final Collector<Object, ?, Object> IGNORING_COLLECTOR = Collector.of(
            Object::new,
            (a, b) -> {
            },
            (a, b) -> a);

    private final ByteBufAllocator alloc;
    private Flo<?, HttpContent> dataFlow;
    private volatile boolean used = false;

    public StreamingRequestBody(ChannelHandlerContext ctx, Flo<?, HttpContent> dataFlow) {
        this.alloc = ctx.alloc();
        this.dataFlow = dataFlow;
    }

    @Override
    public Flo<?, HttpContent> flow() {
        if (used) {
            throw new IllegalStateException("the request body has already been consumed");
        }
        used = true;
        return dataFlow;
    }

    @Override
    public void inspect(UnaryOperator<Flo<?, HttpContent>> inspection) {
        if (used) {
            throw new IllegalStateException("the request body has already been consumed");
        }
        this.dataFlow = inspection.apply(dataFlow);
    }

    @Override
    public Flo<?, ByteBuf> asBuffer() {
        return flow()
                .collect(Collector.of(alloc::compositeBuffer,
                        (composite, content) -> composite.addComponent(true, content.content()),
                        (a, b) -> a.addComponent(true, b)))
                .cast(ByteBuf.class);
    }

    @Override
    public Flo<?, String> asString() {
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
    public <T> Flo<?, T> ignored() {
        return flow()
                .map(c -> {
                    c.release();
                    return null;
                })
                .collect(IGNORING_COLLECTOR)
                .map(c -> null);
    }

    @Override
    public boolean used() {
        return used;
    }
}
