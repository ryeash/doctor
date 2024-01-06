package vest.doctor.ssf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record HttpData(ByteBuffer bytes, boolean last) {

    public static final HttpData EMPTY_LAST_CONTENT = new HttpData(ByteBuffer.wrap(new byte[0]), true);

    public static HttpData copy(ByteBuffer bytes, boolean last) {
        ByteBuffer copy = ByteBuffer.allocate(bytes.remaining());
        copy.put(bytes);
        return new HttpData(copy, last);
    }

    public static HttpData copy(ByteBuffer bytes, int limit, boolean last) {
        if (limit == bytes.remaining()) {
            ByteBuffer copy = ByteBuffer.allocate(bytes.remaining());
            copy.put(bytes);
            return new HttpData(copy, last);
        } else {
            byte[] copy = new byte[limit];
            bytes.get(copy);
            return new HttpData(ByteBuffer.wrap(copy), last);
        }
    }

    public byte[] toByteArray() {
        byte[] buf = new byte[bytes.remaining()];
        bytes.get(buf);
        return buf;
    }

    @Override
    public String toString() {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }
}
