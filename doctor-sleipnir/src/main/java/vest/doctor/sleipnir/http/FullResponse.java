package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;
import java.util.List;

public record FullResponse(StatusLine statusLine,
                           List<Header> headers,
                           ByteBuffer body) implements HttpData {
    @Override
    public ByteBuffer serialize() {
        throw new UnsupportedOperationException();
    }
}
