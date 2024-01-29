package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record Body(ByteBuffer data, boolean last) implements HttpData {
    public static final Body LAST_EMPTY = new Body(ByteBuffer.allocate(0), true);

    public static Body body(String data) {
        return body(data.getBytes(StandardCharsets.UTF_8));
    }

    public static Body body(byte[] data) {
        return new Body(ByteBuffer.wrap(data), false);
    }

    public Body copy() {
        return new Body(BufferUtils.copy(data), last);
    }

    @Override
    public String toString() {
        return "Body{" +
                "data=" + BufferUtils.toString(data) +
                ", last=" + last +
                '}';
    }
}
