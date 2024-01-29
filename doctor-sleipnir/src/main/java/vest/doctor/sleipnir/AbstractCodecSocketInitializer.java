package vest.doctor.sleipnir;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public abstract class AbstractCodecSocketInitializer<IN, OUT> implements SocketInitializer {

    @Override
    public void initialize(ChannelContext channelContext) {
        Flow.Processor<ByteBuffer, IN> decoder = inputDecoder(channelContext);
        channelContext.dataInput().subscribe(decoder);
        handleData(channelContext, decoder, outputEncoder(channelContext));
    }

    protected abstract Flow.Processor<ByteBuffer, IN> inputDecoder(ChannelContext channelContext);

    protected abstract Flow.Subscriber<OUT> outputEncoder(ChannelContext channelContext);

    protected abstract void handleData(ChannelContext channelContext, Flow.Publisher<IN> dataInput, Flow.Subscriber<OUT> dataOutput);

}
