package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;
import java.util.List;

public record FullRequest(RequestLine requestLine,
                          Headers headers,
                          ByteBuffer body) implements HttpData {

    public String getHeader(String name) {
        if (headers.containsKey(name)) {
            List<String> list = headers.get(name);
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    public List<String> getHeaders(String name) {
        return headers.getOrDefault(name, List.of());
    }

    @Override
    public ByteBuffer serialize() {
        throw new UnsupportedOperationException();
    }
}
