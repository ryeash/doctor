package vest.doctor.reactor.http.impl;

import org.reactivestreams.Subscriber;
import vest.doctor.reactor.http.HttpRequest;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;

import java.util.Map;

public record RequestContextImpl(HttpRequest request,
                                 HttpResponse response,
                                 Map<String, Object> attributes) implements RequestContext {
    @Override
    public void attribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
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
    public void subscribe(Subscriber<? super HttpResponse> subscriber) {
        response.publish().subscribe(subscriber);
    }
}
