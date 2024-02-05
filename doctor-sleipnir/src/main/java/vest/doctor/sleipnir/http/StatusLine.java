package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;

public record StatusLine(ProtocolVersion protocolVersion, Status status) implements HttpData {
    @Override
    public ByteBuffer serialize() {
        byte[] prot = protocolVersion.bytes();
        byte[] s = status.bytes();
        ByteBuffer buf = ByteBuffer.allocate(prot.length + s.length + 3);
        buf.put(prot, 0, prot.length);
        buf.put(SPACE);
        buf.put(s, 0, s.length);
        buf.put(CR_LF);
        buf.flip();
        return buf;
    }
}
