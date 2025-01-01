package vest.doctor.apache.server.impl;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import vest.doctor.apache.server.RequestContext;

import java.io.IOException;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class RequestContextWrapper implements RequestContext {
    protected final RequestContext delegate;

    public RequestContextWrapper(RequestContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public String traceId() {
        return delegate.traceId();
    }

    @Override
    public HttpRequest request() {
        return delegate.request();
    }

    @Override
    public EntityDetails entityDetails() {
        return delegate.entityDetails();
    }

    @Override
    public HttpContext context() {
        return delegate.context();
    }

    @Override
    public Flow.Publisher<byte[]> dataInput() {
        return delegate.dataInput();
    }

    @Override
    public void send(AsyncResponseBuilder response) throws HttpException, IOException {
        delegate.send(response);
    }

    @Override
    public void setAttribute(String id, Object obj) {
        delegate.setAttribute(id, obj);
    }

    @Override
    public <T> T getAttribute(String id) {
        return delegate.getAttribute(id);
    }

    @Override
    public String getPathParam(String name) {
        return delegate.getPathParam(name);
    }

    @Override
    public <T> T getPathParam(String name, T defaultValue, Function<String, T> mapper) {
        return delegate.getPathParam(name, defaultValue, mapper);
    }
}
