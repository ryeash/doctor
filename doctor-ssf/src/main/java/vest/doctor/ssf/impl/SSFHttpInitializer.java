package vest.doctor.ssf.impl;

import vest.doctor.rx.FlowBuilder;
import vest.doctor.ssf.Headers;
import vest.doctor.ssf.RequestContext;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.Header;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpInitializer;
import vest.doctor.sleipnir.http.RequestAggregator;

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
                    doRequest(req, dataOutput);
                });
    }

    private void doRequest(FullRequest req, Flow.Subscriber<HttpData> dataOutput) {
        Headers headers = new HeadersImpl();
        for (Header header : req.headers()) {
            headers.add(header.name(), header.value());
        }
        RequestContext ctx = new RequestContextImpl(req.requestLine(), headers, req.body(), dataOutput);
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
}
