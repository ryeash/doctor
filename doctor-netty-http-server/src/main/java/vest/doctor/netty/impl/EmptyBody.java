package vest.doctor.netty.impl;

import io.netty.channel.ChannelHandlerContext;
import vest.doctor.netty.ResponseBody;

public final class EmptyBody implements ResponseBody {
    public static final EmptyBody INSTANCE = new EmptyBody();

    private EmptyBody() {
    }

    @Override
    public void writeTo(ChannelHandlerContext channel) {
        // no-op
    }
}
