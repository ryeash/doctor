package vest.doctor.ssf.impl;

import vest.doctor.rx.FlowBuilder;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpInitializer;
import vest.doctor.sleipnir.http.RequestAggregator;
import vest.doctor.sleipnir.ws.Frame;
import vest.doctor.ssf.RequestContext;

import java.util.concurrent.Flow;

public class SSFHttpInitializer extends HttpInitializer {
    private final HttpConfiguration httpConfiguration;

    public SSFHttpInitializer(HttpConfiguration httpConfiguration) {
        super(httpConfiguration.uriMaxLength(), httpConfiguration.bodyMaxLength());
        this.httpConfiguration = httpConfiguration;
    }

    @Override
    protected void handleData(ChannelContext channelContext, Flow.Publisher<HttpData> dataInput, Flow.Subscriber<HttpData> dataOutput) {
        FlowBuilder.start(dataInput)
                // TODO: gzip
                .chain(new RequestAggregator(channelContext))
                .onNext(req -> {
                    if (req instanceof FullRequest full) {
                        doRequest(channelContext, full, dataOutput);
                    } else if (req instanceof Frame frame) {
                        doFrame(channelContext, frame, dataOutput);
                    }
                });
    }

    private void doRequest(ChannelContext channelContext, FullRequest req, Flow.Subscriber<HttpData> dataOutput) {
        RequestContext ctx = new RequestContextImpl(channelContext, req, dataOutput);
        try {
            httpConfiguration.handler().handle(ctx);
        } catch (Throwable t) {
            httpConfiguration.exceptionHandler().handle(ctx, t);
        } finally {
            if (!ctx.committed()) {
                ctx.send();
            }
        }
    }

    private void doFrame(ChannelContext channelContext, Frame frame, Flow.Subscriber<HttpData> dataOutput) {
        throw new UnsupportedOperationException();
    }
}
