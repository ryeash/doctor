package vest.doctor.ssf;

import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.Headers;
import vest.doctor.sleipnir.http.ProtocolVersion;
import vest.doctor.sleipnir.http.Status;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public interface RequestContext {

    ChannelContext channelContext();

    void attribute(String key, Object value);

    <A> A attribute(String key);

    String method();

    URI uri();

    ProtocolVersion protocolVersion();

    Headers requestHeaders();

    ByteBuffer requestBody();

    Status status();

    void status(Status status);

    Headers responseHeaders();

    ReadableByteChannel responseBody();

    void responseBody(ByteBuffer body);

    default void responseBody(String body) {
        responseBody(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)));
    }

    void send();

    boolean committed();
}
