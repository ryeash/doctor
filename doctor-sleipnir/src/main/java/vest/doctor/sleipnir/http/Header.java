package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record Header(String name, String value) implements HttpData {
    public boolean matches(String name, String value) {
        return this.name.equalsIgnoreCase(name) && this.value.equals(value);
    }

    @Override
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
