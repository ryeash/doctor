package vest.doctor.http.server;

import io.netty.channel.ChannelPipeline;
import vest.doctor.Prioritized;
import vest.doctor.http.server.impl.HttpServerChannelInitializer;

/**
 * Customizes the channel pipeline used for the netty http channels.
 */
@FunctionalInterface
public interface PipelineCustomizer extends Prioritized {

    /**
     * Customize the {@link ChannelPipeline}. Built-in pipeline handler names can be found in
     * {@link HttpServerChannelInitializer}, e.g.
     * {@link HttpServerChannelInitializer#SERVER_HANDLER}.
     *
     * @param channelPipeline the pipeline to customize
     */
    void customize(ChannelPipeline channelPipeline);
}
