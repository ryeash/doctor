package vest.doctor.jaxrs;

import io.netty.bootstrap.ServerBootstrap;

public interface ServerBootstrapCustomizer {

    ServerBootstrap customize(ServerBootstrap serverBootstrap);
}
