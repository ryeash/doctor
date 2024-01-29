package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.ChannelContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import static vest.doctor.sleipnir.http.HttpData.COLON;
import static vest.doctor.sleipnir.http.HttpData.CR_LF;
import static vest.doctor.sleipnir.http.HttpData.SPACE;

public class ResponseEncoder implements Flow.Subscriber<HttpData> {

    private static final byte[] EMPTY_FINAL_CHUNK = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final List<String> FORBIDDEN_HEADERS = List.of("Content-Length", "Transfer-Encoding");
    private static final Header TE_CHUNKED = new Header("Transfer-Encoding", "chunked");
    // TODO trailers???

    private final ChannelContext channelContext;

    public ResponseEncoder(ChannelContext channelContext) {
        this.channelContext = channelContext;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
    }

    @Override
    public void onNext(HttpData data) {
        switch (data) {
            case FullResponse full -> {
                onNext(full.statusLine());
                allowedHeaders(full.headers()).forEach(this::onNext);
                onNext(new Header("Content-Length", Integer.toString(full.body().remaining())));
                subscriber().onNext(ByteBuffer.wrap(CR_LF));
                subscriber().onNext(full.body());
            }
            case StatusLine statusLine -> subscriber().onNext(serialize(statusLine));
            case Header header -> subscriber().onNext(serialize(header));
            case Body body -> {
                // assume it's always in "chunked" encoding
                if (body.last() && !body.data().hasRemaining()) {
                    subscriber().onNext(ByteBuffer.wrap(EMPTY_FINAL_CHUNK));
                } else {
                    String length = Integer.toString(body.data().remaining(), 16);
                    subscriber().onNext(ByteBuffer.wrap((length).getBytes(StandardCharsets.UTF_8)));
                    subscriber().onNext(ByteBuffer.wrap(CR_LF));
                    subscriber().onNext(body.data());
                    subscriber().onNext(ByteBuffer.wrap(CR_LF));
                    if (body.last()) {
                        subscriber().onNext(ByteBuffer.wrap(EMPTY_FINAL_CHUNK));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + data);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        channelContext.dataOutput().onError(throwable);
    }

    @Override
    public void onComplete() {
        channelContext.dataOutput().onComplete();
    }

    private Flow.Subscriber<ByteBuffer> subscriber() {
        return channelContext.dataOutput();
    }

    private Stream<Header> allowedHeaders(List<Header> headers) {
        if (headers == null) {
            return Stream.empty();
        } else {
            return headers.stream()
                    .filter(header -> FORBIDDEN_HEADERS.stream().noneMatch(header.name()::equalsIgnoreCase));
        }
    }

    private static ByteBuffer serialize(StatusLine statusLine) {
        byte[] prot = statusLine.protocolVersion().bytes();
        byte[] status = statusLine.status().bytes();
        ByteBuffer buf = ByteBuffer.allocate(prot.length + status.length + 3);
        buf.put(prot, 0, prot.length);
        buf.put(SPACE);
        buf.put(status, 0, status.length);
        buf.put(CR_LF);
        buf.flip();
        return buf;
    }

    private static ByteBuffer serialize(Header header) {
        byte[] n = header.name().getBytes(StandardCharsets.UTF_8);
        byte[] v = header.value().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(n.length + v.length + 3);
        buf.put(n);
        buf.put(COLON);
        buf.put(v);
        buf.put(CR_LF);
        buf.flip();
        return buf;
    }

}
