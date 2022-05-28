package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.concurrent.PromiseNotifier;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Flo;

import java.util.concurrent.Flow;

public class FloResponseBody implements ResponseBody {

    private final Flow.Processor<?, HttpContent> processor;

    public FloResponseBody(Flow.Processor<?, HttpContent> processor) {
        this.processor = processor;
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        ChannelPromise channelPromise = channel.newPromise();
        Flo.from(processor)
                .map(channel::write)
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
                f.addListener(new PromiseNotifier<>(channelPromise));
            }
        }
    }
}
