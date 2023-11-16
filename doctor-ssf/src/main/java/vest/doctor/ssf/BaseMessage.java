package vest.doctor.ssf;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import static vest.doctor.ssf.impl.Headers.CONNECTION;
import static vest.doctor.ssf.impl.Headers.CONTENT_LENGTH;
import static vest.doctor.ssf.impl.Headers.CONTENT_TYPE;
import static vest.doctor.ssf.impl.Headers.DATE;
import static vest.doctor.ssf.impl.Headers.HOST;
import static vest.doctor.ssf.impl.Headers.SERVER;
import static vest.doctor.ssf.impl.Headers.TRANSFER_ENCODING;

public interface BaseMessage {
    void addHeader(String keyValueHeaderString);

    void addHeader(String headerName, String headerValue);

    void setHeader(String headerName, String headerValue);

    void removeHeader(String headerName);

    String getHeader(String headerName);

    List<String> getHeaders(String headerName);

    void eachHeader(BiConsumer<String, String> consumer);

    Collection<String> headerNames();

    int numHeaders();

    default void host(String host) {
        setHeader(HOST, host);
    }

    default int contentLength() {
        String contentLength = getHeader(CONTENT_LENGTH);
        if (contentLength != null) {
            return Integer.parseInt(contentLength);
        }
        return 0;
    }

    default void contentLength(int length) {
        setHeader(CONTENT_LENGTH, String.valueOf(length));
    }

    default void date(String date) {
        setHeader(DATE, date);
    }

    default void server(String server) {
        setHeader(SERVER, server);
    }

    default void contentType(String type) {
        setHeader(CONTENT_TYPE, type);
    }

    default String transferEncoding() {
        return getHeader(TRANSFER_ENCODING);
    }

    default String connection() {
        return getHeader(CONNECTION);
    }
}
