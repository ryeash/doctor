package vest.doctor.grpc;

import io.grpc.Server;
import io.grpc.protobuf.services.HealthStatusManager;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;

import java.util.concurrent.TimeUnit;

/**
 * A handle to the gRPC server instance created by {@link GrpcFactory#grpcServerFactory(ProviderRegistry, ConfigurationFacade, HealthStatusManager)}.
 * The underlying {@link Server} returned by {@link #getServer()} may be null, indicating that the server
 * was never started (due to configuration).
 */
public final class GrpcServerHandle implements AutoCloseable {
    private final Server server;

    public GrpcServerHandle(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.shutdown();
            server.awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}
