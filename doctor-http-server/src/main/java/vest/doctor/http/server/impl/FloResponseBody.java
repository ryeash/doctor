package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.concurrent.PromiseNotifier;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Flo;

public class FloResponseBody implements ResponseBody {

    private final Flo<?, HttpContent> flow;

    public FloResponseBody(Flo<?, HttpContent> flow) {
        this.flow = flow;
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        ChannelPromise channelPromise = channel.newPromise();
        flow.map(channel::write)
                .whenComplete(channel::flush)
                .subscribe()
                .future()
                .whenComplete(new JoinFP(channelPromise)::signal);
        return channelPromise;
    }

    record JoinFP(ChannelPromise channelPromise) {
        void signal(ChannelFuture f, Throwable error) {
            if (error != null) {
                channelPromise.setFailure(error);
            } else {
                channelPromise.setSuccess();
                f.addListener(new PromiseNotifier<>(channelPromise));
            }
        }
    }
}
