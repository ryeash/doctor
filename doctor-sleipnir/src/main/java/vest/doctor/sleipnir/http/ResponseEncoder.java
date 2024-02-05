package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.ByteBufferReadableByteChannel;
import vest.doctor.sleipnir.ChannelContext;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import static vest.doctor.sleipnir.http.HttpData.CR_LF;

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
                out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                out().onNext(new ByteBufferReadableByteChannel(full.body()));
            }
            case Body body -> {
                // assume it's always in "chunked" encoding
                // if the chunk is empty but not the last, just skip it
                if (!body.last() && !body.data().hasRemaining()) {
                    return;
                }
                if (body.last() && !body.data().hasRemaining()) {
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(EMPTY_FINAL_CHUNK)));
                } else {
                    String length = Integer.toString(body.data().remaining(), 16);
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap((length).getBytes(StandardCharsets.UTF_8))));
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                    out().onNext(new ByteBufferReadableByteChannel(body.data()));
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                    if (body.last()) {
                        out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(EMPTY_FINAL_CHUNK)));
                    }
                }
            }
            default -> out().onNext(new HttpDataReadableChannel(data));
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

    private Flow.Subscriber<ReadableByteChannel> out() {
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
}
