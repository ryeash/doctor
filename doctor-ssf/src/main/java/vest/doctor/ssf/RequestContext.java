package vest.doctor.ssf;

import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.Headers;
import vest.doctor.sleipnir.http.Status;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public interface RequestContext {

    ChannelContext channelContext();

    void attribute(String key, Object value);

    <A> A attribute(String key);

    FullRequest request();

    FullResponse response();

    String method();

    URI uri();

    Headers requestHeaders();

    ByteBuffer requestBody();

    void status(Status status);

    Headers responseHeaders();

    void responseBody(ByteBuffer body);

    default void responseBody(String body) {
        responseBody(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)));
    }

    void responseBody(ReadableByteChannel readableByteChannel);

    void send();

    boolean committed();
}
