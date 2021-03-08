package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.http.server.ResponseBody;

public final class EmptyBody implements ResponseBody {
    public static final EmptyBody INSTANCE = new EmptyBody();

    private EmptyBody() {
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        return channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
    }
}
