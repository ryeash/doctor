package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestBody;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerRequest implements Request {

    private final HttpRequest request;
    private final URI uri;
    private final String path;
    private final QueryStringDecoder queryStringDecoder;
    private final RequestBody body;

    private Map<String, Cookie> cookies;

    public ServerRequest(HttpRequest request, RequestBody body) {
        this.request = request;
        this.uri = URI.create(request.uri());
        this.path = uri.getRawPath();
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        this.body = body;
    }

    @Override
    public HttpRequest unwrap() {
        return request;
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();
    }

    @Override
    public String header(CharSequence headerName) {
        return request.headers().get(headerName);
    }

    @Override
    public Cookie cookie(String name) {
        return cookies().get(name);
    }

    @Override
    public Map<String, Cookie> cookies() {
        if (cookies == null) {
            cookies = new HashMap<>();
            request.headers().getAll(HttpHeaderNames.COOKIE)
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .flatMap(cookie -> ServerCookieDecoder.LAX.decode(cookie).stream())
                    .forEach(cookie -> cookies.put(cookie.name(), cookie));
            cookies = Collections.unmodifiableMap(cookies);
        }
        return cookies;
    }

    @Override
    public RequestBody body() {
        return body;
    }

    @Override
    public MultiPartData multiPartBody() {
        return new MultiPartDataImpl(request, body);
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
    public List<String> queryParams(String name) {
        return queryStringDecoder.parameters().get(name);
    }

    @Override
    public Charset requestCharset(Charset defaultCharset) {
        return HttpUtil.getCharset(request, defaultCharset);
    }

    @Override
    public Charset responseCharset(Charset defaultCharset) {
        // look for Accept-Charset
        String acceptCharset = request.headers().get(HttpHeaderNames.ACCEPT_CHARSET);
        if (acceptCharset != null) {
            String[] split = divide(acceptCharset, ',');
            for (String s : split) {
                String[] charsetAndQ = divide(s, ';');
                String charset = charsetAndQ[0].trim();
                if (Charset.isSupported(charset)) {
                    return Charset.forName(charset);
                }
            }
        }

        // look for Accept
        String accept = request.headers().get(HttpHeaderNames.ACCEPT);
        if (accept != null) {
            return HttpUtil.getCharset(accept, defaultCharset);
        }

        // fallback to the default
        return defaultCharset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method()).append(' ').append(uri()).append(' ').append(request.protocolVersion());
        sb.append("\n");
        for (Map.Entry<String, String> entry : headers()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if (body != null) {
            sb.append(body);
        }
        return sb.toString();
    }

    public static String[] divide(String str, char delimiter) {
        int i = str.indexOf(delimiter);
        if (i >= 0) {
            return new String[]{str.substring(0, i), str.substring(i + 1)};
        } else {
            return new String[]{str, null};
        }
    }
}
