package vest.doctor.netty.impl;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

public class EmptyBody implements ResponseBody {
    public static final EmptyBody INSTANCE = new EmptyBody();

    private EmptyBody() {
    }

    @Override
    public HttpContent toContent(Request request, Response response) {
        return new DefaultHttpContent(Unpooled.EMPTY_BUFFER);
    }
}
