package demo.app.reactor;

import jakarta.inject.Singleton;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.logging.AccessLogFactory;

@Singleton
public class TestServerCustomizer implements vest.doctor.reactor.http.HttpServerCustomizer {
    @Override
    public HttpServer customize(HttpServer httpServer) {
        return httpServer.accessLog(false, AccessLogFactory.createFilter(p -> true));
    }
}
