package vest.doctor.netty.impl;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

import static io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT;

public class ServerResponse implements Response {

    private final Request request;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final HttpHeaders headers = new DefaultHttpHeaders(false);
    private ResponseBody body = EmptyBody.INSTANCE;

    public ServerResponse(Request request) {
        this.request = request;
    }

    @Override
    public Response status(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public Response status(int status) {
        this.status = HttpResponseStatus.valueOf(status);
        return this;
    }

    @Override
    public Response status(int status, String reasonString) {
        this.status = HttpResponseStatus.valueOf(status, reasonString);
        return this;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public Response header(CharSequence name, Object value) {
        headers.set(name, value);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Response setCookie(String name, String value) {
        String cookieValue = STRICT.encode(new io.netty.handler.codec.http.cookie.DefaultCookie(name, value));
        header(HttpHeaderNames.SET_COOKIE, cookieValue);
        return this;
    }

    @Override
    public Response body(ResponseBody body) {
        if (body == null) {
            this.body = EmptyBody.INSTANCE;
        } else {
            this.body = body;
        }
        return this;
    }

    @Override
    public ResponseBody body() {
        return body;
    }

    @Override
    public Request request() {
        return request;
    }
}
