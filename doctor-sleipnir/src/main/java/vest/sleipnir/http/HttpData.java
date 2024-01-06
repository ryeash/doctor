package vest.sleipnir.http;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface HttpData {
    byte CR = 13;
    byte LF = 10;
    byte[] CR_LF = new byte[]{CR, LF};
    byte COLON = ':';
    byte SPACE = ' ';
    String HTTP1_1 = "HTTP/1.1";

    static String httpDate() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    static Body body(String data) {
        return body(data.getBytes(StandardCharsets.UTF_8));
    }

    static Body body(byte[] data) {
        return new HttpData.Body(ByteBuffer.wrap(data), false);
    }

    record Header(String name, String value) implements HttpData {
        public boolean matches(String name, String value) {
            return this.name.equalsIgnoreCase(name) && this.value.equals(value);
        }

        public ByteBuffer serialize() {
            byte[] n = name.getBytes(StandardCharsets.UTF_8);
            byte[] v = value.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(n.length + v.length + 3);
            buf.put(n);
            buf.put(COLON);
            buf.put(v);
            buf.put(CR_LF);
            buf.flip();
            return buf;
        }
    }

    record Body(ByteBuffer data, boolean last) implements HttpData {
        public static final Body LAST_EMPTY = new Body(ByteBuffer.allocate(0), true);

        public ByteBuffer serialize() {
            return data;
        }
    }

    record RequestLine(String method, URI uri, String protocolVersion) implements HttpData {
        public ByteBuffer serialize() {
            return ByteBuffer.wrap((method + " " + uri + " " + protocolVersion).getBytes(StandardCharsets.UTF_8));
        }
    }

    record StatusLine(String protocolVersion, Status status) implements HttpData {
        public ByteBuffer serialize() {
            byte[] prot = protocolVersion.getBytes(StandardCharsets.US_ASCII);
            byte[] status = status().bytes();
            ByteBuffer buf = ByteBuffer.allocate(prot.length + status.length + 3);
            buf.put(prot, 0, prot.length);
            buf.put(SPACE);
            buf.put(status, 0, status.length);
            buf.put(CR_LF);
            buf.flip();
            return buf;
        }
    }

    record Response(StatusLine statusLine, List<Header> headers) implements HttpData {
    }

    record FullRequest(RequestLine requestLine,
                       List<Header> headers,
                       ByteBuffer body) implements HttpData {

        public String method() {
            return requestLine.method();
        }

        public String protocolVersion() {
            return requestLine.protocolVersion();
        }

        public URI uri() {
            return requestLine.uri();
        }

        public String getHeader(String name) {
            for (Header header : headers) {
                if (header.name().equalsIgnoreCase(name)) {
                    return header.value();
                }
            }
            return null;
        }

        public List<String> getHeaders(String name) {
            return headers.stream()
                    .filter(header -> header.name().equalsIgnoreCase(name))
                    .map(Header::value)
                    .toList();
        }
    }

    record FullResponse(StatusLine statusLine,
                        List<Header> headers,
                        ByteBuffer body) implements HttpData {
    }
}
