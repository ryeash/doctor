package vest.doctor.ssf.impl;

import vest.doctor.ssf.Request;
import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public final class RequestContextImpl implements RequestContext {

    private final Request request;
    private final Response response;
    private final Map<String, Object> attributes;

    public RequestContextImpl(Request request, Response response) {
        this.request = request;
        this.response = response;
        this.attributes = new ConcurrentSkipListMap<>();
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Response response() {
        return response;
    }

    @Override
    public void attribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name) {
        return (T) attributes.get(name);
    }
}
