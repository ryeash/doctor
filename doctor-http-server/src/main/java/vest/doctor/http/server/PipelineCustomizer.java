package vest.doctor.http.server;

import io.netty.channel.ChannelPipeline;
import vest.doctor.Prioritized;
import vest.doctor.http.server.impl.HttpServerChannelInitializer;

/**
 * A listener that will get notified for various events in the netty http server.
 */
public interface PipelineCustomizer extends Prioritized {

    /**
     * Customize the {@link ChannelPipeline}. Built in pipeline handler names can be found in
     * {@link HttpServerChannelInitializer}, e.g.
     * {@link HttpServerChannelInitializer#SERVER_HANDLER}.
     *
     * @param channelPipeline the pipeline to customize
     */
    void customize(ChannelPipeline channelPipeline);
}
