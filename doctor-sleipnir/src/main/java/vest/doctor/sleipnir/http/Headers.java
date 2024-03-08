package vest.doctor.sleipnir.http;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class Headers extends TreeMap<String, List<String>> {
    // names
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String HOST = "Host";
    public static final String SERVER = "Server";
    public static final String DATE = "Date";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
    public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
    public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
    public static final String UPGRADE = "Upgrade";
    // values
    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_JSON = "application/json";
    public static final String OCTET_STREAM = "application/octet-stream";
    public static final String GZIP = "gzip";
    public static final String DEFLATE = "deflate";
    public static final String WEBSOCKET = "websocket";

    public Headers() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public void set(String name, String value) {
        remove(name);
        if (value != null) {
            add(name, value);
        }
    }

    public void add(Header header) {
        add(header.name(), header.value());
    }

    public void add(String name, String value) {
        computeIfAbsent(name, v -> new LinkedList<>()).add(value);
    }

    public String getFirst(String name) {
        List<String> values = get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        } else {
            return null;
        }
    }

    public List<String> getAll(String name) {
        return getOrDefault(name, List.of());
    }
}
