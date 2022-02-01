package vest.doctor.jersey;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import vest.doctor.netty.HttpServerChannelInitializer;
import vest.doctor.netty.PipelineCustomizer;

record HttpAggregatorCustomizer(int maxLength) implements PipelineCustomizer {

    public static final String HTTP_AGGREGATOR = "httpAggregator";

    @Override
    public void customize(ChannelPipeline channelPipeline) {
        channelPipeline.addBefore(HttpServerChannelInitializer.SERVER_HANDLER, HTTP_AGGREGATOR, new HttpObjectAggregator(maxLength, true));
    }
}
