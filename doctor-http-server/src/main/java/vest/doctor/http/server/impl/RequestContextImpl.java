package vest.doctor.http.server.impl;

import io.netty.channel.ChannelHandlerContext;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;

public class RequestContextImpl implements RequestContext {

    private final Request request;
    private final Response response;
    private final ChannelHandlerContext channelContext;
    private final Map<String, Object> attributes;

    public RequestContextImpl(Request request, Response response, ChannelHandlerContext channelContext) {
        this.request = request;
        this.response = response;
        this.channelContext = channelContext;
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
    public ChannelHandlerContext channelContext() {
        return channelContext;
    }

    @Override
    public ExecutorService pool() {
        return channelContext.executor();
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attributeOrElse(String name, T orElse) {
        return (T) attributes.getOrDefault(name, orElse);
    }

    @Override
    public Set<String> attributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method()).append(' ').append(request.uri()).append('\n');
        for (Map.Entry<String, String> header : request.headers()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
        }
        sb.append('\n');
        sb.append(request.body());
        return sb.toString();
    }
}
