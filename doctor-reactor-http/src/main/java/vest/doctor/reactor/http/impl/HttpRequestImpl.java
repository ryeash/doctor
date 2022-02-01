package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;
import vest.doctor.reactor.http.HttpRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpRequestImpl implements HttpRequest {

    private final HttpServerRequest request;
    private final QueryStringDecoder queryStringDecoder;
    private final Map<CharSequence, Cookie> cookies;

    public HttpRequestImpl(HttpServerRequest request) {
        this.request = request;
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        this.cookies = request.requestHeaders()
                .getAll(HttpHeaderNames.COOKIE)
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(cookie -> ServerCookieDecoder.LAX.decode(cookie).stream())
                .collect(Collectors.toUnmodifiableMap(Cookie::name, Function.identity()));
    }

    @Override
    public HttpServerRequest unwrap() {
        return request;
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    public URI uri() {
        return URI.create(request.uri());
    }

    @Override
    public String path() {
        return request.path();
    }

    @Override
    public HttpHeaders headers() {
        return request.requestHeaders();
    }

    @Override
    public String header(CharSequence name) {
        return request.requestHeaders().get(name);
    }

    @Override
    public String pathParam(String name) {
        return request.param(name);
    }

    @Override
    public String queryParam(String name) {
        List<String> params = queryStringDecoder.parameters().get(name);
        if (params != null && !params.isEmpty()) {
            return params.get(0);
        }
        return null;
    }

    @Override
    public Cookie cookie(String name) {
        return cookies().get(name);
    }

    @Override
    public Map<CharSequence, Cookie> cookies() {
        return cookies;
    }

    @Override
    public ByteBufFlux body() {
        return request.receive();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method()).append(" ").append(request.uri()).append(" ").append(request.version()).append("\n\r");
        for (Map.Entry<String, String> requestHeader : request.requestHeaders()) {
            sb.append(requestHeader.getKey()).append(": ").append(requestHeader.getValue()).append("\n\r");
        }
        return sb.toString();
    }
}
