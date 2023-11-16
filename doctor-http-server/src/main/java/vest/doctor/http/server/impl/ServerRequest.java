package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import vest.doctor.http.server.Part;
import vest.doctor.http.server.Request;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerRequest implements Request {

    private final FullHttpRequest request;
    private final URI uri;
    private final String path;
    private final QueryStringDecoder queryStringDecoder;

    private Map<String, Cookie> cookies;

    public ServerRequest(FullHttpRequest request) {
        this.request = request;
        this.uri = URI.create(request.uri());
        this.path = uri.getRawPath();
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
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
    public ByteBuf body() {
        return request.content();
    }

    @Override
    public List<Part> multiPartBody() {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        if (!decoder.isMultipart()) {
            throw new IllegalStateException("not a multipart request");
        }
        List<Part> parts = new LinkedList<>();
        try {
            decoder.offer(new DefaultLastHttpContent(request.content()));
            InterfaceHttpData next;
            while ((next = decoder.next()) != null) {
                String type = next.getHttpDataType().name();
                String name = next.getName();
                switch (next.getHttpDataType()) {
                    case Attribute, InternalAttribute -> {
                        Attribute attribute = (Attribute) next;
                        parts.add(new PartImpl(type, name, attribute.content().retainedDuplicate(), false));
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        parts.add(new PartImpl(type, name, fileUpload.content().retain(), false));
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            parts.add(new PartImpl("", "", Unpooled.EMPTY_BUFFER, true));
        } finally {
            decoder.destroy();
        }
        return parts;
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
        sb.append(request.content());
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
