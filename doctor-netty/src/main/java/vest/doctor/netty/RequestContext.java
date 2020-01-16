package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT;

/**
 * Represents an HTTP request/response state.
 */
public class RequestContext {

    private static Logger log = LoggerFactory.getLogger(RequestContext.class);

    private ChannelHandlerContext ctx;
    // request
    private FullHttpRequest request;
    private Map<String, Cookie> cookies;
    private URI requestUri;
    private QueryStringDecoder queryStringDecoder;
    private Map<String, String> pathParams;
    private String requestPath;

    // response
    private HttpResponseStatus status;
    private HttpHeaders responseHeaders;
    private ByteBuf responseBody;

    private boolean halted = false;
    private Map<String, Object> attributes;

    private CompletableFuture<Void> future;

    RequestContext(ChannelHandlerContext ctx, FullHttpRequest request) {
        this.ctx = ctx;
        this.request = request;

        this.requestUri = URI.create(request.uri());
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        this.requestPath = requestUri.getRawPath();

        this.status = HttpResponseStatus.OK;
        this.responseHeaders = new DefaultHttpHeaders(false);
        this.responseHeaders.add(HttpHeaderNames.SERVER, "ssf");
        this.responseHeaders.set(HttpHeaderNames.DATE, new Date());
        this.future = new CompletableFuture<>();
        responseBody(Unpooled.EMPTY_BUFFER);
    }

    /**
     * Get the channel context for this request, i.e. the client tcp connection.
     */
    public ChannelHandlerContext channelContext() {
        return ctx;
    }

    /**
     * Get the HTTP request method; ex: GET
     */
    public HttpMethod requestMethod() {
        return request.method();
    }

    /**
     * Get the request uri: ex: /path?query=something
     */
    public URI requestUri() {
        return requestUri;
    }

    /**
     * Get the request path: /path/to/resource
     */
    public String requestPath() {
        return requestPath;
    }

    /**
     * Alter the request path. This can be used to redirect a request in a BEFORE_MATCH filter to an entirely different endpoint.
     *
     * @param path The new path to route this request with
     */
    public void rewriteRequestPath(String path) {
        this.requestPath = path;
    }

    /**
     * Get the HTTP request headers.
     */
    public HttpHeaders requestHeaders() {
        return request.headers();
    }

    /**
     * Get a request cookie.
     *
     * @param name The name of cookie
     * @return The cookie object associated with the given name, or null if none exists
     */
    public Cookie cookie(String name) {
        return cookies().get(name);
    }

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

    /**
     * Get the request body as a {@link ByteBuf}.
     */
    public ByteBuf requestBody() {
        return request.content();
    }

    public InputStream requestBodyStream() {
        return requestBodyStream(true);
    }

    /**
     * Get the request body as an InputStream. Compression will be detected automatically and the returned stream will
     * handle deflating the stream if necessary.
     */
    public InputStream requestBodyStream(boolean detectCompression) {
        if (detectCompression && requestHeaders().get(HttpHeaderNames.CONTENT_ENCODING, "").contains(HttpHeaderValues.GZIP)) {
            try {
                return new GZIPInputStream(new ByteBufInputStream(request.content()));
            } catch (IOException e) {
                throw new UncheckedIOException("error reading gzip'd request body", e);
            }
        } else {
            return new ByteBufInputStream(request.content());
        }
    }

    /**
     * Internal use. Set the routing path params derived from the route path and request path.
     *
     * @param pathParams The map of path parameters
     */
    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    /**
     * Get the path parameters set for this request context.
     */
    public Map<String, String> pathParams() {
        return pathParams;
    }

    /**
     * Get the value for a path parameter.
     *
     * @param name The path parameter name to get the value for
     * @return The associated value for the path parameter, or null if none exists
     */
    public String pathParam(String name) {
        return pathParams.get(name);
    }

    /**
     * Get the value for a query parameter.
     *
     * @param name The query parameter name to get the value for
     * @return The associated value for the query parameter, or null if none exists
     */
    public String queryParam(String name) {
        List<String> params = queryStringDecoder.parameters().get(name);
        if (params != null && !params.isEmpty()) {
            return params.get(0);
        }
        return null;
    }

    /**
     * Get all the values for a query parameter.
     *
     * @param name The query parameter name to get the values for
     * @return All values associated with the give query parameter, or an empty list if none exists
     */
    public List<String> queryParams(String name) {
        return queryStringDecoder.parameters().getOrDefault(name, null);
    }

    /**
     * Get the halted state for the context. A halted RequestContext will not be further processed by the Router.
     */
    public boolean isHalted() {
        return halted;
    }

    /**
     * Halts this RequestContext with a 307 Temporary Redirect to the given location (using the Location header).
     *
     * @param location The location to redirect to
     */
    public void redirect(String location) {
        redirect(HttpResponseStatus.TEMPORARY_REDIRECT, location);
    }

    /**
     * Halts this RequestContext with the give status and sets the Location header to the given value.
     *
     * @param status   The status to use for the redirect, must be one of the 3xx series status codes (like 307 or 308)
     * @param location The location to redirect to
     */
    public void redirect(HttpResponseStatus status, String location) {
        if (status.code() < 300 || status.code() > 399) {
            throw new IllegalArgumentException("redirect status codes must be in the range [300, 399]");
        }
        responseHeader(HttpHeaderNames.LOCATION, location);
        halt(status, Unpooled.EMPTY_BUFFER);
    }

    /**
     * Halts this RequestContext, setting the status to the given value.
     *
     * @param status The response status to set
     * @see RequestContext#halt(HttpResponseStatus, ByteBuf)
     */
    public void halt(int status) {
        halt(HttpResponseStatus.valueOf(status), Unpooled.EMPTY_BUFFER);
    }

    /**
     * Halts this RequestContext, setting the status and body to the given values.
     *
     * @param status  The response status to set
     * @param message The string to set as the response body
     */
    public void halt(int status, String message) {
        halt(HttpResponseStatus.valueOf(status), message != null ? Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.UTF_8)) : Unpooled.EMPTY_BUFFER);
    }

    /**
     * Halts this RequestContext, setting the status and body to the given values.
     *
     * @param status  The response status to set
     * @param message The string to set as the response body
     */
    public void halt(HttpResponseStatus status, String message) {
        halt(status, message != null ? Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.UTF_8)) : Unpooled.EMPTY_BUFFER);
    }

    /**
     * Halts this RequestContext, setting the response status and body to the given values. Halting the context prevents
     * subsequent routes from processing the request and the response is returned to the client in the state that it is in
     * immediately after returning from this method.
     *
     * @param status The response status to set
     * @param body   The response body to set
     */
    public void halt(HttpResponseStatus status, ByteBuf body) {
        halted = true;
        response(status, body);
        complete();
    }

    /**
     * Attach an attribute to this RequestContext. An attribute is an arbitrary object that can be carried through filters and routes
     * to share request state.
     *
     * @param name      The name of the attribute
     * @param attribute The object to attach to the request
     */
    public void attribute(String name, Object attribute) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(name, attribute);
    }

    /**
     * Retrieve an affix value by name.
     *
     * @param name The name of the affix
     * @return The affixed value associated with the given name, or null if none exists
     */
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name) {
        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(name);
    }

    /**
     * Retrieve an attribute value by type.
     *
     * @param type The type of the affix object to retrieve
     * @return The affixed value associated with the given type, or null if none exists. If multiple attributes match the
     * given type, there are no guarantees as to which one will be returned.
     */
    public <T> T attribute(Class<T> type) {
        for (Object o : attributes.values()) {
            if (type.isInstance(o)) {
                return type.cast(o);
            }
        }
        return null;
    }

    /**
     * Set the response status and body.
     *
     * @param status The status to set
     * @param body   The response body to set
     */
    public void response(int status, String body) {
        responseStatus(status);
        responseBody(body);
    }

    /**
     * Set the response status and body.
     *
     * @param status The status to set
     * @param body   The response body to set
     */
    public void response(HttpResponseStatus status, String body) {
        responseStatus(status);
        responseBody(body);
    }

    /**
     * Set the response status and body.
     *
     * @param status The status to set
     * @param body   The body to set
     */
    public void response(HttpResponseStatus status, ByteBuf body) {
        responseStatus(status);
        responseBody(body);
    }

    /**
     * Set the response status.
     */
    public void responseStatus(HttpResponseStatus status) {
        this.status = status;
    }

    /**
     * Set the response status.
     */
    public void responseStatus(int status) {
        this.status = HttpResponseStatus.valueOf(status);
    }

    /**
     * Get the response status
     */
    public HttpResponseStatus responseStatus() {
        return status;
    }

    /**
     * Set a response header.
     *
     * @param name  The header name
     * @param value The header value
     */
    public void responseHeader(CharSequence name, Object value) {
        this.responseHeaders.set(name, value);
    }

    /**
     * Get the HttpHeaders instance that stores the response headers.
     */
    public HttpHeaders responseHeaders() {
        return responseHeaders;
    }

    /**
     * Set a cookie to be sent to the client
     *
     * @param name  The cookie name
     * @param value The cookie value
     */
    public void setCookie(String name, String value) {
        String cookieValue = STRICT.encode(new io.netty.handler.codec.http.cookie.DefaultCookie(name, value));
        responseHeader(HttpHeaderNames.SET_COOKIE, cookieValue);
    }

    /**
     * Set the response body.
     */
    public void responseBody(ByteBuf body) {
        responseBody = body != null ? body : Unpooled.EMPTY_BUFFER;
        responseHeader(HttpHeaderNames.CONTENT_LENGTH, responseBody.readableBytes());
    }

    /**
     * Set the response body to the given String; the string will be encoded using UTF-8.
     */
    public void responseBody(String body) {
        responseBody(body, StandardCharsets.UTF_8);
    }

    /**
     * Set the response body to the given String.
     *
     * @param body    The response string to set
     * @param charset The charset to use to encode the string to a byte array
     */
    public void responseBody(String body, Charset charset) {
        if (body != null && !body.isEmpty()) {
            responseBody(body.getBytes(charset));
        } else {
            responseBody(Unpooled.EMPTY_BUFFER);
        }
    }

    /**
     * Consume the given InputStream and set the response body to the consumed bytes.
     *
     * @param body The InputStream to consume bytes from
     */
    public void responseBody(InputStream body) {
        try {
            ByteBuf byteBuf = Unpooled.buffer(4096);
            byte[] buf = new byte[4096];
            int read;
            while ((read = body.read(buf)) != -1) {
                byteBuf.writeBytes(buf, 0, read);
            }
            responseBody(byteBuf);
        } catch (IOException e) {
            throw new UncheckedIOException("error sinking input stream to ByteBuf", e);
        } finally {
            try {
                body.close();
            } catch (IOException e) {
                log.trace("ignored", e);
            }
        }
    }

    /**
     * Set the response body to the given bytes.
     */
    public void responseBody(byte[] body) {
        responseBody(Unpooled.wrappedBuffer(body));
    }

    /**
     * Get the async status of this request.
     *
     * @see #future()
     */
    public boolean isComplete() {
        return future.isDone();
    }

    /**
     * Get the CompletableFuture representing the future completion of this request.
     */
    public CompletableFuture<Void> future() {
        return future;
    }

    public void complete() {
        future.complete(null);
    }

    public void complete(Throwable t) {
        Objects.requireNonNull(t, "may not complete with a null throwable");
        future.completeExceptionally(t);
    }

    HttpResponse buildResponse() {
        return new DefaultFullHttpResponse(HTTP_1_1, status, responseBody, responseHeaders, EmptyHttpHeaders.INSTANCE);
    }

    @Override
    public String toString() {
        return "Request: [" + request.method() + ' ' + request.uri() + ' ' + request + ']' +
                "\nPathParams: " + pathParams +
                "\nResponseStatus: " + status +
                "\nResponseHeaders: " + responseHeaders +
                "\nResponseBody: " + responseBody +
                "\nHalted: " + halted +
                "\nAttribute: " + attributes;
    }

    /**
     * Get the charset that the client is expecting based on request headers.
     *
     * @param defaultCharset The default charset to use if the client didn't explicitly ask for one
     * @return The charset that the client wants the server to encode with
     */
    public Charset getResponseCharset(Charset defaultCharset) {
        // look for Accept-Charset
        String acceptCharset = request.headers().get(HttpHeaderNames.ACCEPT_CHARSET);
        if (acceptCharset != null) {
            String[] split = acceptCharset.split(",");
            for (String s : split) {
                List<String> charsetAndQ = Arrays.asList(s.split(";"));
                String charset = charsetAndQ.get(0).trim();
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

    /**
     * Get the charset of the request body based on the Content-Type header of the request.
     *
     * @param defaultCharset The default charset to use if the client didn't indicate one
     * @return The charset that the client request indicated
     */
    public Charset getRequestCharset(Charset defaultCharset) {
        return HttpUtil.getCharset(request, defaultCharset);
    }

}