package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BaseProcessor;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.ws.WebsocketDecoder;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class HTTPDecoder extends BaseProcessor<ByteBuffer, HttpData> {

    public static final String WS_UPGRADED = "doctor.sleipnir.websocketHandshakeComplete";
    private final ChannelContext channelContext;
    private final RequestDecoder requestDecoder;
    private final WebsocketDecoder websocketDecoder;

    public HTTPDecoder(ChannelContext channelContext, int maxLineLength, long maxBodyLength) {
        this.channelContext = channelContext;
        this.requestDecoder = new RequestDecoder(maxLineLength, maxBodyLength);
        this.websocketDecoder = new WebsocketDecoder(maxBodyLength);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super HttpData> subscriber) {
        super.subscribe(subscriber);
        requestDecoder.subscribe(subscriber);
        websocketDecoder.subscribe(subscriber);
    }

    @Override
    public void onNext(ByteBuffer item) {
        if (isWebsocket()) {
            websocketDecoder.onNext(item);
        } else {
            requestDecoder.onNext(item);
        }
    }

    private boolean isWebsocket() {
        Boolean b = (Boolean) channelContext.attributes().get(WS_UPGRADED);
        return b != null && b;
    }
}
