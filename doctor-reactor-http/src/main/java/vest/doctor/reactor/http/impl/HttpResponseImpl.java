package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerResponse;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.ResponseBody;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HttpResponseImpl implements HttpResponse {

    private final HttpServerResponse response;
    private ResponseBody body;

    public HttpResponseImpl(HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public HttpServerResponse unwrap() {
        return response;
    }

    @Override
    public HttpResponse status(int status) {
        response.status(status);
        return this;
    }

    @Override
    public HttpResponse status(HttpResponseStatus status) {
        response.status(status);
        return this;
    }

    @Override
    public HttpResponse header(CharSequence name, CharSequence value) {
        response.header(name, value);
        return this;
    }

    @Override
    public HttpResponse header(CharSequence name, Iterable<? extends CharSequence> value) {
        response.responseHeaders().set(name, value);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return response.responseHeaders();
    }

    @Override
    public HttpResponse body(ResponseBody body) {
        this.body = Objects.requireNonNull(body);
        return this;
    }

    @Override
    public HttpResponse cookie(Cookie cookie) {
        response.cookies().put(cookie.name(), Set.of(cookie));
        return this;
    }

    @Override
    public Map<CharSequence, Set<Cookie>> cookies() {
        return response.cookies();
    }

    @Override
    public Publisher<Void> send() {
        return response.sendObject(body != null ? body.content() : ResponseBody.empty().content());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(response.version() + " " + response.status() + "\n\r");
        for (Map.Entry<String, String> header : response.responseHeaders()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n\r");
        }
        if (body != null) {
            sb.append(body);
        }
        return sb.toString();
    }
}
