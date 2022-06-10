package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.concurrent.PromiseNotifier;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

public class FloResponseBody implements ResponseBody {

    private final Rx<? extends HttpContent> processor;

    public FloResponseBody(Flow.Publisher<? extends HttpContent> processor) {
        this.processor = Rx.from(processor);
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        ChannelPromise channelPromise = channel.newPromise();
        Rx.from(processor)
                .map(channel::write)
                .runOnComplete(channel::flush)
                .subscribe()
                .whenComplete(new JoinFP(channelPromise));
        return channelPromise;
    }

    record JoinFP(ChannelPromise channelPromise) implements BiConsumer<ChannelFuture, Throwable> {
        @Override
        public void accept(ChannelFuture f, Throwable error) {
            if (error != null) {
                channelPromise.setFailure(error);
            } else {
                f.addListener(new PromiseNotifier<>(channelPromise));
            }
        }
    }
}
