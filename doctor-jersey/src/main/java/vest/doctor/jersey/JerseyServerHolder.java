package vest.doctor.jersey;

import vest.doctor.netty.NettyHttpServer;

public record JerseyServerHolder(DoctorJerseyContainer container,
                                 NettyHttpServer httpServer) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        if (container != null) {
            container.close();
        }
        if (httpServer != null) {
            httpServer.close();
        }
    }
}
