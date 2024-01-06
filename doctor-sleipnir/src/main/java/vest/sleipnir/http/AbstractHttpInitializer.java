package vest.sleipnir.http;

import vest.sleipnir.AbstractCodecSocketInitializer;
import vest.sleipnir.Channel;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public abstract class AbstractHttpInitializer extends AbstractCodecSocketInitializer<HttpData, HttpData> {

    public AbstractHttpInitializer() {
        super(true);
    }

    @Override
    protected final Flow.Processor<ByteBuffer, HttpData> inputDecoder(Channel channel) {
        return new RequestDecoder(2048, 8 * 1024 * 1024);
    }

    @Override
    protected final Flow.Processor<HttpData, ByteBuffer> outputEncoder(Channel channel) {
        return new ResponseEncoder(channel);
    }
}
