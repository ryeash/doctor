package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.AbstractCodecSocketInitializer;
import vest.doctor.sleipnir.ChannelContext;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public abstract class HttpInitializer extends AbstractCodecSocketInitializer<HttpData, HttpData> {
    private final int uriMaxLength;
    private final int maxBodyLength;

    public HttpInitializer(int uriMaxLength, int maxBodyLength) {
        this.uriMaxLength = uriMaxLength;
        this.maxBodyLength = maxBodyLength;
    }

    @Override
    protected Flow.Processor<ByteBuffer, HttpData> inputDecoder(ChannelContext channelContext) {
        return new RequestDecoder(uriMaxLength, maxBodyLength);
    }

    @Override
    protected Flow.Subscriber<HttpData> outputEncoder(ChannelContext channelContext) {
        return new ResponseEncoder(channelContext);
    }
}
