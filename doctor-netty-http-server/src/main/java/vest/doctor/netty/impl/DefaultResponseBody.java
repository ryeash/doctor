package vest.doctor.netty.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

public class DefaultResponseBody implements ResponseBody {
    private final ByteBuf buf;

    public DefaultResponseBody(ByteBuf buf) {
        this.buf = buf != null ? buf : Unpooled.EMPTY_BUFFER;
    }

    @Override
    public HttpContent toContent(Request request, Response response) {
        return new DefaultHttpContent(buf);
    }
}
