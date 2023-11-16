package vest.doctor.ssf;

import vest.doctor.ssf.impl.Headers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public interface Request extends BaseMessage {

    String schemaVersion();

    String method();

    URI uri();

    byte[] body();

    default InputStream bodyStream() {
        byte[] data = body();
        if (data == null || data.length == 0) {
            return InputStream.nullInputStream();
        }
        for (String value : getHeaders(Headers.CONTENT_ENCODING)) {
            if (value.contains(Headers.GZIP)) {
                try {
                    return new GZIPInputStream(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (value.contains(Headers.DEFLATE)) {
                return new InflaterInputStream(new ByteArrayInputStream(data));
            }
        }
        return new ByteArrayInputStream(data);
    }
}
