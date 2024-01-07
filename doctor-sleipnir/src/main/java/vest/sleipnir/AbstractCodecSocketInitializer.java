package vest.sleipnir;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public abstract class AbstractCodecSocketInitializer<IN, OUT> implements SocketInitializer {

    @Override
    public final Flow.Publisher<ByteBuffer> initialize(ChannelContext channelContext) {
        Flow.Publisher<IN> input = subscribe(channelContext.dataInput(), inputDecoder(channelContext));
        return subscribe(handleData(channelContext, input), outputEncoder(channelContext));
    }

    protected abstract Flow.Processor<ByteBuffer, IN> inputDecoder(ChannelContext channelContext);

    protected abstract Flow.Processor<OUT, ByteBuffer> outputEncoder(ChannelContext channelContext);

    protected abstract Flow.Publisher<OUT> handleData(ChannelContext channelContext, Flow.Publisher<IN> dataInput);

    protected static <I, O> Flow.Publisher<O> subscribe(Flow.Publisher<I> publisher, Flow.Processor<I, O> processor) {
        publisher.subscribe(processor);
        return processor;
    }
}
