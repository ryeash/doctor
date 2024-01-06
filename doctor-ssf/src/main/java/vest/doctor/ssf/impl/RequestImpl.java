package vest.doctor.ssf.impl;

import vest.doctor.ssf.HttpData;
import vest.doctor.ssf.Request;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public final class RequestImpl extends BaseMessage implements Request {
    private ExecutorService pool;
    private String schemaVersion;
    private String method;
    private URI uri;
    private Flow.Processor<HttpData, HttpData> body;

    public RequestImpl() {
        super();
    }

    public RequestImpl(ExecutorService pool, String schemaVersion, String method, URI uri) {
        super();
        this.pool = pool;
        this.schemaVersion = schemaVersion;
        this.method = method;
        this.uri = uri;
        this.body = new SubmissionPublisher<>(pool, Flow.defaultBufferSize()); // TODO
    }

    @Override
    public ExecutorService pool() {
        return pool;
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
    public Flow.Publisher<HttpData> bodyFlow() {
        return body;
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
            sb.append("\n   Body: ").append(body);
        }
        return sb.toString();
    }
}
