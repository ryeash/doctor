package vest.sleipnir;

import vest.doctor.rx.FlowBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.function.Function;

public abstract class AbstractCodecSocketInitializer<IN, OUT> implements SocketInitializer {

    private final boolean copyInputBuffer;

    protected AbstractCodecSocketInitializer(boolean copyInputBuffer) {
        this.copyInputBuffer = copyInputBuffer;
    }

    @Override
    public final Flow.Publisher<ByteBuffer> initialize(Channel channel) {
        FlowBuilder<IN> input = FlowBuilder.start(channel.dataInput())
                .onNext(copyInputBuffer ? BufferUtils::copy : Function.identity())
                .chain(inputDecoder(channel));
        return FlowBuilder.start(handleData(channel, input))
                .chain(outputEncoder(channel));
    }

    protected abstract Flow.Processor<ByteBuffer, IN> inputDecoder(Channel channel);

    protected abstract Flow.Processor<OUT, ByteBuffer> outputEncoder(Channel channel);

    protected abstract Flow.Publisher<OUT> handleData(Channel channel, Flow.Publisher<IN> dataInput);
}
