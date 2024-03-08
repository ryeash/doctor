package vest.doctor.ssf.impl;

import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.Headers;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.ssf.RequestContext;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestContextImpl implements RequestContext {

    private final ChannelContext channelContext;
    private final Map<String, Object> attributes;
    private final Flow.Subscriber<HttpData> dataOutput;

    private final FullRequest fullRequest;
    private final FullResponse fullResponse;

    private final AtomicBoolean committed = new AtomicBoolean(false);

    public RequestContextImpl(ChannelContext channelContext, FullRequest fullRequest, Flow.Subscriber<HttpData> dataOutput) {
        this.channelContext = channelContext;
        this.attributes = new HashMap<>();
        this.fullRequest = fullRequest;
        this.dataOutput = dataOutput;

        this.fullResponse = new FullResponse();
        fullResponse.status(Status.OK);
        fullResponse.headers().set("Date", HttpData.httpDate());
        fullResponse.headers().set("Server", "ssf");
    }

    @Override
    public ChannelContext channelContext() {
        return channelContext;
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
    public FullRequest request() {
        return fullRequest;
    }

    @Override
    public FullResponse response() {
        return fullResponse;
    }

    @Override
    public String method() {
        return fullRequest.requestLine().method();
    }

    @Override
    public URI uri() {
        return fullRequest.requestLine().uri();
    }

    @Override
    public Headers requestHeaders() {
        return fullRequest.headers();
    }

    @Override
    public ByteBuffer requestBody() {
        return fullRequest.body();
    }

    @Override
    public void status(Status status) {
        this.fullResponse.status(status);
    }

    @Override
    public Headers responseHeaders() {
        return fullResponse.headers();
    }

    @Override
    public void responseBody(ByteBuffer body) {
        fullResponse.body(body);
    }

    @Override
    public void responseBody(ReadableByteChannel readableByteChannel) {
        fullResponse.body(readableByteChannel);
    }

    @Override
    public void send() {
        if (committed.compareAndSet(false, true)) {
            dataOutput.onNext(fullResponse);
        } else {
            throw new IllegalStateException("response already committed");
        }
    }

    @Override
    public boolean committed() {
        return committed.get();
    }
}
