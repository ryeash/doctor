package vest.sleipnir.http;

import vest.sleipnir.BaseProcessor;
import vest.sleipnir.ChannelContext;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

public class ResponseEncoder extends BaseProcessor<HttpData, ByteBuffer> {

    private static final byte[] EMPTY_FINAL_CHUNK = "0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final List<String> FORBIDDEN_HEADERS = List.of("Content-Length", "Transfer-Encoding");
    private static final HttpData.Header TE_CHUNKED = new HttpData.Header("Transfer-Encoding", "chunked");
    // TODO trailers???

    private final ChannelContext channelContext;

    public ResponseEncoder(ChannelContext channelContext) {
        this.channelContext = channelContext;
    }

    @Override
    public void onNext(HttpData data) {
        switch (data) {
            case HttpData.FullResponse full -> {
                onNext(full.statusLine());
                allowedHeaders(full.headers()).forEach(this::onNext);
                onNext(TE_CHUNKED);
                subscriber().onNext(ByteBuffer.wrap(HttpData.CR_LF));
                onNext(new HttpData.Body(full.body(), true));
            }
            case HttpData.Response response -> {
                onNext(response.statusLine());
                allowedHeaders(response.headers()).forEach(this::onNext);
                onNext(TE_CHUNKED);
                subscriber().onNext(ByteBuffer.wrap(HttpData.CR_LF));
            }
            case HttpData.StatusLine statusLine -> subscriber().onNext(statusLine.serialize());
            case HttpData.Header header -> subscriber().onNext(header.serialize());
            case HttpData.Body body -> {
                if (body.last() && !body.data().hasRemaining()) {
                    subscriber().onNext(ByteBuffer.wrap(EMPTY_FINAL_CHUNK));
                } else {
                    String length = Integer.toString(body.data().remaining(), 16);
                    subscriber().onNext(ByteBuffer.wrap((length).getBytes(StandardCharsets.UTF_8)));
                    subscriber().onNext(ByteBuffer.wrap(HttpData.CR_LF));
                    subscriber().onNext(body.data());
                    subscriber().onNext(ByteBuffer.wrap(HttpData.CR_LF));
                    if (body.last()) {
                        subscriber().onNext(ByteBuffer.wrap(EMPTY_FINAL_CHUNK));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + data);
        }

        SelectionKey key = channelContext.socketChannel().keyFor(channelContext.selector());
        key.interestOps(SelectionKey.OP_WRITE | key.interestOps());
    }

    private Stream<HttpData.Header> allowedHeaders(List<HttpData.Header> headers) {
        if (headers == null) {
            return Stream.empty();
        } else {
            return headers.stream()
                    .filter(header -> FORBIDDEN_HEADERS.stream().noneMatch(header.name()::equalsIgnoreCase));
        }
    }

}
