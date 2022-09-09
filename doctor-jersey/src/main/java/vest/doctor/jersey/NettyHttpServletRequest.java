package vest.doctor.jersey;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import vest.doctor.netty.HttpServerChannelInitializer;

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thin wrapper around a netty {@link HttpRequest} to support injecting an {@link HttpServletRequest}
 * into handler methods. Not all functionality is supported.
 */
final class NettyHttpServletRequest implements HttpServletRequest {

    private final ChannelHandlerContext ctx;
    private final HttpRequest request;
    private final QueryStringDecoder queryStringDecoder;
    private final Map<String, Object> attributes = new HashMap<>();

    NettyHttpServletRequest(ChannelHandlerContext ctx, HttpRequest request) {
        this.ctx = ctx;
        this.request = request;
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return request.headers().getAll(HttpHeaderNames.COOKIE)
                .stream()
                .map(ServerCookieDecoder.LAX::decodeAll)
                .flatMap(List::stream)
                .map(nettyCookie -> new Cookie(nettyCookie.name(), nettyCookie.value()))
                .toArray(Cookie[]::new);
    }

    @Override
    public long getDateHeader(String s) {
        return request.headers().getTimeMillis(s, -1L);
    }

    @Override
    public String getHeader(String s) {
        return request.headers().get(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        List<String> all = request.headers().getAll(s);
        return Collections.enumeration(all);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public int getIntHeader(String s) {
        return request.headers().getInt(s, -1);
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public String getPathInfo() {
        return queryStringDecoder.path();
    }

    @Override
    public String getPathTranslated() {
        return queryStringDecoder.path();
    }

    @Override
    public String getContextPath() {
        return queryStringDecoder.path();
    }

    @Override
    public String getQueryString() {
        return queryStringDecoder.rawQuery();
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(request.uri());
    }

    @Override
    public String getServletPath() {
        return queryStringDecoder.path();
    }

    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String s) {
        return attributes.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return HttpUtil.getCharset(request).name();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public int getContentLength() {
        return HttpUtil.getContentLength(request, -1);
    }

    @Override
    public long getContentLengthLong() {
        return HttpUtil.getContentLength(request, -1L);
    }

    @Override
    public String getContentType() {
        return HttpUtil.getMimeType(request).toString();
    }

    @Override
    public ServletInputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParameter(String s) {
        List<String> list = queryStringDecoder.parameters().get(s);
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(queryStringDecoder.parameters().keySet());
    }

    @Override
    public String[] getParameterValues(String s) {
        List<String> list = queryStringDecoder.parameters().get(s);
        if (list == null || list.isEmpty()) {
            return new String[0];
        } else {
            return list.toArray(String[]::new);
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return queryStringDecoder.parameters()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(String[]::new)));
    }

    @Override
    public String getProtocol() {
        return request.protocolVersion().protocolName();
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        SocketAddress socketAddress = ctx.channel().localAddress();
        if (socketAddress instanceof InetSocketAddress inet) {
            return inet.getPort();
        } else {
            return -1;
        }
    }

    @Override
    public BufferedReader getReader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddr() {
        return ctx.channel().remoteAddress().toString();
    }

    @Override
    public String getRemoteHost() {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress inet) {
            return inet.getHostName();
        } else {
            return socketAddress.toString();
        }
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        attributes.remove(s);
    }

    @Override
    public Locale getLocale() {
        String str = request.headers().get(HttpHeaderNames.ACCEPT_LANGUAGE);
        if (str != null && !str.isEmpty()) {
            return Locale.forLanguageTag(str.contains(";") ? str.substring(0, str.indexOf(';')) : str);
        } else {
            return Locale.getDefault();
        }
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<Locale> collect = request.headers().getAll(HttpHeaderNames.ACCEPT_LANGUAGE)
                .stream()
                .map(str -> str.split(","))
                .flatMap(Stream::of)
                .map(str -> Locale.forLanguageTag(str.contains(";") ? str.substring(0, str.indexOf(';')) : str))
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return Collections.enumeration(List.of(Locale.getDefault()));
        } else {
            return Collections.enumeration(collect);
        }
    }

    @Override
    public boolean isSecure() {
        return ctx.pipeline().get(HttpServerChannelInitializer.SSL_CONTEXT) != null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemotePort() {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress inet) {
            return inet.getPort();
        } else {
            return -1;
        }
    }

    @Override
    public String getLocalName() {
        SocketAddress socketAddress = ctx.channel().localAddress();
        if (socketAddress instanceof InetSocketAddress inet) {
            return inet.getHostName();
        } else {
            return null;
        }
    }

    @Override
    public String getLocalAddr() {
        return ctx.channel().localAddress().toString();
    }

    @Override
    public int getLocalPort() {
        SocketAddress socketAddress = ctx.channel().localAddress();
        if (socketAddress instanceof InetSocketAddress inet) {
            return inet.getPort();
        } else {
            return -1;
        }
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestId() {
        return ctx.channel().id().toString();
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return new ServletConnection() {
            @Override
            public String getConnectionId() {
                return ctx.channel().id().toString();
            }

            @Override
            public String getProtocol() {
                return request.protocolVersion().protocolName();
            }

            @Override
            public String getProtocolConnectionId() {
                return "";
            }

            @Override
            public boolean isSecure() {
                return ctx.pipeline().names().contains(HttpServerChannelInitializer.SSL_CONTEXT);
            }
        };
    }
}
