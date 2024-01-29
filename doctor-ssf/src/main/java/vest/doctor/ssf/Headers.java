package vest.doctor.ssf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Headers extends Iterable<Map.Entry<String, String>>{
    String AUTHORIZATION = "Authorization";
    String CONTENT_LENGTH = "Content-Length";
    String HOST = "Host";
    String SERVER = "Server";
    String DATE = "Date";
    String CONTENT_TYPE = "Content-Type";
    String CONTENT_ENCODING = "Content-Encoding";
    String TRANSFER_ENCODING = "Transfer-Encoding";
    String CONNECTION = "Connection";
    String TEXT_PLAIN = "text/plain";
    String APPLICATION_JSON = "application/json";
    String OCTET_STREAM = "application/octet-stream";
    String GZIP = "gzip";
    String DEFLATE = "deflate";

    void add(String headerName, String headerValue);

    void set(String headerName, String headerValue);

    void remove(String headerName);

    String get(String headerName);

    List<String> getAll(String headerName);

    Collection<String> headerNames();

    default void host(String host) {
        set(HOST, host);
    }

    default int contentLength() {
        String contentLength = get(CONTENT_LENGTH);
        if (contentLength != null) {
            return Integer.parseInt(contentLength);
        }
        return -1;
    }

    default void contentLength(int length) {
        set(CONTENT_LENGTH, String.valueOf(length));
    }

    default void date(String date) {
        set(DATE, date);
    }

    default void server(String server) {
        set(SERVER, server);
    }

    default void contentType(String type) {
        set(CONTENT_TYPE, type);
    }

    default String transferEncoding() {
        return get(TRANSFER_ENCODING);
    }

    default String connection() {
        return get(CONNECTION);
    }
}
