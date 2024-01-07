package vest.sleipnir.http;

import vest.sleipnir.AbstractCodecSocketInitializer;
import vest.sleipnir.ChannelContext;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public abstract class AbstractHttpInitializer extends AbstractCodecSocketInitializer<HttpData, HttpData> {

    @Override
    protected final Flow.Processor<ByteBuffer, HttpData> inputDecoder(ChannelContext channelContext) {
        return new RequestDecoder(2048, 8 * 1024 * 1024);
    }

    @Override
    protected final Flow.Processor<HttpData, ByteBuffer> outputEncoder(ChannelContext channelContext) {
        return new ResponseEncoder(channelContext);
    }
}
