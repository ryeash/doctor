package vest.doctor.sleipnir.http;

import java.net.URI;
import java.nio.ByteBuffer;

public record RequestLine(String method, URI uri, ProtocolVersion protocolVersion) implements HttpData {
    @Override
    public ByteBuffer serialize() {
        throw new UnsupportedOperationException();
    }
}
