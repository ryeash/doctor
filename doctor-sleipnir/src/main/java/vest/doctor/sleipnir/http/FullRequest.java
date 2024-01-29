package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;
import java.util.List;

public record FullRequest(RequestLine requestLine, List<Header> headers, ByteBuffer body) {
}
