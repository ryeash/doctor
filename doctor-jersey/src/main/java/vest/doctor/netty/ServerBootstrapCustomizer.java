package vest.doctor.netty;

import io.netty.bootstrap.ServerBootstrap;
import vest.doctor.Prioritized;

/**
 * Customizes the server bootstrap used to initialize the netty http server.
 */
@FunctionalInterface
public interface ServerBootstrapCustomizer extends Prioritized {

    /**
     * Customize the {@link ServerBootstrap}.
     *
     * @param serverBootstrap the bootstrap to customize
     */
    void customize(ServerBootstrap serverBootstrap);
}
