package vest.doctor.netty.common;

import io.netty.channel.ChannelPipeline;
import vest.doctor.Prioritized;

/**
 * Customizes the channel pipeline used for the netty http channels.
 */
@FunctionalInterface
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
