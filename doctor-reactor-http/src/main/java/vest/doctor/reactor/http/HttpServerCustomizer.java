package vest.doctor.reactor.http;

import reactor.netty.http.server.HttpServer;

/**
 * Customizes the {@link HttpServer} before it starts up.
 */
public interface HttpServerCustomizer {

    /**
     * Customize the server.
     *
     * @param httpServer the server to customize
     */
    void customize(HttpServer httpServer);
}
