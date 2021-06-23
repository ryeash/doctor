package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.util.Objects;

public class ServerResponse implements Response {

    private final Request request;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final HttpHeaders headers = new DefaultHttpHeaders(false);
    private ResponseBody body = EmptyBody.INSTANCE;

    public ServerResponse(Request request) {
        this.request = request;
        headers.set(HttpHeaderNames.SERVER, "doctor-netty");
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
    public Response body(ResponseBody body) {
        this.body = Objects.requireNonNullElse(body, EmptyBody.INSTANCE);
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
