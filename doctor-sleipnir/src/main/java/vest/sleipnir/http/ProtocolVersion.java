package vest.sleipnir.http;

import java.nio.charset.StandardCharsets;

// TODO
public class ProtocolVersion {
    public static final ProtocolVersion HTTP1_0 = new ProtocolVersion("HTTP/1.0");
    public static final ProtocolVersion HTTP1_1 = new ProtocolVersion("HTTP/1.1");

    private final String protocolVersion;
    private final byte[] bytes;

    public ProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.bytes = protocolVersion.getBytes(StandardCharsets.UTF_8);
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public byte[] bytes() {
        return bytes;
    }
}
