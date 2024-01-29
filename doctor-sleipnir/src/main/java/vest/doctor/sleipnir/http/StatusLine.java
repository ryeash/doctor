package vest.doctor.sleipnir.http;

public record StatusLine(ProtocolVersion protocolVersion, Status status) implements HttpData {
}
