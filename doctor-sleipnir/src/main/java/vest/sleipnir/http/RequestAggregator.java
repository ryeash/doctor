package vest.sleipnir.http;

import vest.sleipnir.BaseProcessor;
import vest.sleipnir.BufferUtils;
import vest.sleipnir.ChannelContext;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class RequestAggregator extends BaseProcessor<HttpData, HttpData.FullRequest> {

    private static final String REQ_AGG_ATTR = "doctor.sleipnir.requestAggregator";
    private final ChannelContext channelContext;

    public RequestAggregator(ChannelContext channelContext) {
        this.channelContext = channelContext;
    }

    @Override
    public void onNext(HttpData item) {
        switch (item) {
            case HttpData.RequestLine rl ->
                    channelContext.attributes().put(REQ_AGG_ATTR, new Agg(rl, new LinkedList<>(), new LinkedList<>()));
            case HttpData.Header h -> ((Agg) channelContext.attributes().get(REQ_AGG_ATTR)).headers.add(h);
            case HttpData.Body b -> {
                Agg agg = (Agg) channelContext.attributes().get(REQ_AGG_ATTR);
                agg.body.add(b);
                if (b.last()) {
                    channelContext.attributes().remove(REQ_AGG_ATTR);
                    subscriber().onNext(new HttpData.FullRequest(agg.requestLine, new LinkedList<>(agg.headers), aggregateBody(agg.body)));
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + item);
        }
    }

    private ByteBuffer aggregateBody(List<HttpData.Body> body) {
        if (body == null || body.isEmpty()) {
            return ByteBuffer.wrap(new byte[0]);
        }
        int totalSize = body.stream().map(HttpData.Body::data).mapToInt(Buffer::remaining).sum();
        ByteBuffer agg = ByteBuffer.allocate(totalSize);
        for (HttpData.Body httpData : body) {
            BufferUtils.transfer(httpData.data(), agg);
        }
        agg.flip();
        return agg;
    }

    record Agg(HttpData.RequestLine requestLine,
               List<HttpData.Header> headers,
               List<HttpData.Body> body) {
    }
}
