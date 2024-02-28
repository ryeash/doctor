package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.ByteBufferReadableByteChannel;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.ws.Frame;
import vest.doctor.sleipnir.ws.FrameHeader;
import vest.doctor.sleipnir.ws.Payload;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Flow;

import static vest.doctor.sleipnir.http.HttpData.CR_LF;

public class ResponseEncoder implements Flow.Subscriber<HttpData> {

    private static final byte[] EMPTY_FINAL_CHUNK = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final Set<String> FORBIDDEN_HEADERS;

    static {
        FORBIDDEN_HEADERS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        FORBIDDEN_HEADERS.add("Content-Length");
        FORBIDDEN_HEADERS.add("Transfer-Encoding");
    }

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

                full.headers().forEach((name, values) -> {
                    if (!FORBIDDEN_HEADERS.contains(name)) {
                        onNext(new Header(name, String.join(",", values)));
                    }
                });

                if (full.body() == null || full.body() == BufferUtils.EMPTY_CHANNEL) {
                    onNext(new Header("Content-Length", "0"));
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                } else if (full.body() instanceof ByteBufferReadableByteChannel buffer) {
                    onNext(new Header("Content-Length", Integer.toString(buffer.length())));
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                    out().onNext(buffer);
                } else {
                    onNext(TE_CHUNKED);
                    out().onNext(new ByteBufferReadableByteChannel(ByteBuffer.wrap(CR_LF)));
                    out().onNext(full.body());
                }
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
            case Frame frame -> {
                onNext(frame.getHeader());
                onNext(frame.getPayload());
            }
            case FrameHeader frameHeader -> {
                out().onNext(new ByteBufferReadableByteChannel(frameHeader.serialize()));
            }
            case Payload payload -> {
                out().onNext(new ByteBufferReadableByteChannel(payload.serialize()));
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
}
