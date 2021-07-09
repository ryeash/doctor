package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.RxRequest;
import vest.doctor.http.server.RxResponse;

import java.util.Date;
import java.util.Objects;

public class RxServerResponse implements RxResponse {

    private final RxRequest request;

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final HttpHeaders headers = new DefaultHttpHeaders(false);
    private ResponseBody body = EmptyBody.INSTANCE;

    public RxServerResponse(RxRequest request) {
        this.request = request;
        headers.set(HttpHeaderNames.SERVER, "doctor-netty");
        headers.set(HttpHeaderNames.DATE, new Date());
    }

    @Override
    public RxResponse status(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public RxResponse status(int status) {
        this.status = HttpResponseStatus.valueOf(status);
        return this;
    }

    @Override
    public RxResponse status(int status, String reasonString) {
        this.status = HttpResponseStatus.valueOf(status, reasonString);
        return this;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public RxResponse header(CharSequence name, Object value) {
        headers.set(name, value);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public RxResponse body(ResponseBody body) {
        this.body = Objects.requireNonNullElse(body, EmptyBody.INSTANCE);
        return this;
    }

    @Override
    public ResponseBody body() {
        return body;
    }

    @Override
    public RxRequest request() {
        return request;
    }
}
