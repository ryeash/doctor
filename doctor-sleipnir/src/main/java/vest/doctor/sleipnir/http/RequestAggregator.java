package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BaseProcessor;
import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.ws.Frame;
import vest.doctor.sleipnir.ws.FrameHeader;
import vest.doctor.sleipnir.ws.Payload;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestAggregator extends BaseProcessor<HttpData, HttpData> {

    private static final String REQ_AGG_ATTR = "doctor.sleipnir.requestAggregator";
    private static final String FRAME_AGG_ATTR = "doctor.sleipnir.frameAggregator";
    private final ChannelContext channelContext;

    public RequestAggregator(ChannelContext channelContext) {
        this.channelContext = channelContext;
    }

    @Override
    public void onNext(HttpData item) {
        switch (item) {
            case RequestLine rl ->
                    channelContext.attributes().put(REQ_AGG_ATTR, new Agg(rl, new Headers(), new LinkedList<>(), new AtomicInteger(0)));
            case Header h -> ((Agg) channelContext.attributes().get(REQ_AGG_ATTR)).headers.add(h);
            case Body b -> {
                Agg agg = (Agg) channelContext.attributes().get(REQ_AGG_ATTR);
                agg.body.add(BufferUtils.copy(b.data()));
                if (b.last()) {
                    channelContext.attributes().remove(REQ_AGG_ATTR);
                    subscriber().onNext(new FullRequest(agg.requestLine, agg.headers, aggregateBody(agg.body)));
                }
            }

            case FrameHeader frameHeader ->
                    channelContext.attributes().put(FRAME_AGG_ATTR, new FrameAgg(frameHeader, new LinkedList<>()));
            case Payload payload -> {
                FrameAgg agg = (FrameAgg) channelContext.attributes().get(FRAME_AGG_ATTR);
                agg.payload.add(BufferUtils.copy(payload.getData()));
                if (payload.isLast()) {
                    channelContext.attributes().remove(REQ_AGG_ATTR);
                    subscriber().onNext(new Frame(agg.header, aggregateBody(agg.payload)));
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + item);
        }
    }

    private ByteBuffer aggregateBody(List<ByteBuffer> body) {
        if (body == null || body.isEmpty()) {
            return BufferUtils.EMPTY_BUFFER;
        }
        int totalSize = body.stream()
                .mapToInt(Buffer::remaining)
                .sum();
        ByteBuffer agg = ByteBuffer.allocate(totalSize);
        for (ByteBuffer httpData : body) {
            BufferUtils.transfer(httpData, agg);
        }
        agg.flip();
        return agg;
    }

    record Agg(RequestLine requestLine,
               Headers headers,
               List<ByteBuffer> body,
               AtomicInteger totalBodySize) {
    }

    record FrameAgg(FrameHeader header,
                    List<ByteBuffer> payload) {
    }
}
