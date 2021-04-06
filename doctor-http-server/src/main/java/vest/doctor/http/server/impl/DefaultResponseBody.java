package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.http.server.ResponseBody;

public class DefaultResponseBody implements ResponseBody {
    private final ByteBuf buf;

    public DefaultResponseBody(ByteBuf buf) {
        this.buf = buf != null ? buf : Unpooled.EMPTY_BUFFER;
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        channel.write(new DefaultHttpContent(buf));
        return channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
    }
}
