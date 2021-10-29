package vest.doctor.netty.common;

import io.netty.bootstrap.ServerBootstrap;

/**
 * Customizes the server bootstrap used to initialize the netty http server.
 */
@FunctionalInterface
public interface ServerBootstrapCustomizer {

    /**
     * Customize the {@link ServerBootstrap}.
     *
     * @param serverBootstrap the bootstrap to customize
     */
    void customize(ServerBootstrap serverBootstrap);
}
