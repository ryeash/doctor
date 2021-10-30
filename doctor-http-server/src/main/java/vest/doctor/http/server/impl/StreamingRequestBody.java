package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.http.server.RequestBody;
import vest.doctor.workflow.Workflow;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A handle to the HTTP request body. Supports asynchronously reading the body data.
 */
public class StreamingRequestBody implements RequestBody {
    private final ByteBufAllocator alloc;
    private final Workflow<?, HttpContent> dataFlow;
    private volatile boolean used = false;

    public StreamingRequestBody(ChannelHandlerContext ctx, Workflow<?, HttpContent> dataFlow) {
        this.alloc = ctx.alloc();
        this.dataFlow = dataFlow;
    }

    @Override
    public Workflow<?, HttpContent> flow() {
        used = true;
        return dataFlow;
    }

    @Override
    public Workflow<?, ByteBuf> asBuffer() {
        return flow()
                .collect(Collector.of(alloc::compositeBuffer,
                        (composite, content) -> composite.addComponent(true, content.content()),
                        (a, b) -> a.addComponent(true, b)))
                .map(cbb -> cbb);
    }

    @Override
    public Workflow<?, String> asString() {
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
    public Workflow<?, Void> ignored() {
        return flow()
                .map(c -> {
                    c.release();
                    return null;
                });
    }

    @Override
    public boolean used() {
        return used;
    }
}
