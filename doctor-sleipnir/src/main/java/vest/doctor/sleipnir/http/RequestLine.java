package vest.doctor.sleipnir.http;

import java.net.URI;

public record RequestLine(String method, URI uri, ProtocolVersion protocolVersion) implements HttpData {
}
