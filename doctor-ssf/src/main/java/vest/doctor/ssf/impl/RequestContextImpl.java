package vest.doctor.ssf.impl;

import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.Header;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.ProtocolVersion;
import vest.doctor.sleipnir.http.RequestLine;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.sleipnir.http.StatusLine;
import vest.doctor.ssf.Headers;
import vest.doctor.ssf.RequestContext;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestContextImpl implements RequestContext {

    private final Map<String, Object> attributes;
    private final RequestLine requestLine;
    private final Headers requestHeaders;
    private final ByteBuffer requestBody;
    private final Flow.Subscriber<HttpData> dataOutput;

    private Status status;
    private final Headers responseHeaders;
    private ByteBuffer responseBody;

    private final AtomicBoolean committed = new AtomicBoolean(false);

    public RequestContextImpl(RequestLine requestLine, Headers requestHeaders, ByteBuffer requestBody, Flow.Subscriber<HttpData> dataOutput) {
        this.attributes = new HashMap<>();
        this.requestLine = requestLine;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.dataOutput = dataOutput;

        this.status = Status.OK;
        this.responseHeaders = new HeadersImpl();
        responseHeaders.add("Date", HttpData.httpDate());
        responseHeaders.add("Server", "ssf");
        this.responseBody = BufferUtils.EMPTY_BUFFER;
    }

    @Override
    public void attribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A attribute(String key) {
        return (A) attributes.get(key);
    }

    @Override
    public String method() {
        return requestLine.method();
    }

    @Override
    public URI uri() {
        return requestLine.uri();
    }

    @Override
    public ProtocolVersion protocolVersion() {
        return requestLine.protocolVersion();
    }

    @Override
    public Headers requestHeaders() {
        return requestHeaders;
    }

    @Override
    public ByteBuffer requestBody() {
        return requestBody;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void status(Status status) {
        this.status = status;
    }

    @Override
    public Headers responseHeaders() {
        return responseHeaders;
    }

    @Override
    public ByteBuffer responseBody() {
        return responseBody;
    }

    @Override
    public void responseBody(ByteBuffer body) {
        this.responseBody = body;
    }

    @Override
    public void send() {
        if (committed.compareAndSet(false, true)) {
            List<Header> headers = new LinkedList<>();
            for (Map.Entry<String, String> entry : responseHeaders) {
                headers.add(new Header(entry.getKey(), entry.getValue()));
            }
            FullResponse full = new FullResponse(new StatusLine(ProtocolVersion.HTTP1_1, status), headers, responseBody != null ? responseBody : BufferUtils.EMPTY_BUFFER);
            dataOutput.onNext(full);
        } else {
            throw new IllegalStateException("response already committed");
        }
    }

    @Override
    public boolean committed() {
        return committed.get();
    }
}
