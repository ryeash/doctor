package vest.doctor.ssf.impl;

import vest.doctor.ssf.Request;

import java.net.URI;

public final class RequestImpl extends Headers implements Request {
    private String schemaVersion;
    private String method;
    private URI uri;
    private byte[] body;

    public RequestImpl() {
        super();
    }

    public RequestImpl(String schemaVersion, String method, URI uri) {
        super();
        this.schemaVersion = schemaVersion;
        this.method = method;
        this.uri = uri;
    }

    @Override
    public String schemaVersion() {
        return schemaVersion;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public byte[] body() {
        return body;
    }

    void body(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(' ');
        sb.append(uri);
        sb.append("\n   Headers:");
        eachHeader((key, value) -> {
            sb.append("\n      ").append(key).append(" : ").append(value);
        });
        if (body != null) {
            sb.append("\n   Body: ").append(new String(body));
        }
        return sb.toString();
    }
}
