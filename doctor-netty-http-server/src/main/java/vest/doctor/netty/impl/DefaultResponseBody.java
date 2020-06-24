package vest.doctor.netty.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import vest.doctor.netty.ResponseBody;

public class DefaultResponseBody implements ResponseBody {
    private final ByteBuf buf;

    public DefaultResponseBody(ByteBuf buf) {
        this.buf = buf != null ? buf : Unpooled.EMPTY_BUFFER;
    }

    @Override
    public void writeTo(ChannelHandlerContext channel) {
        channel.write(new DefaultHttpContent(buf));
    }
}
