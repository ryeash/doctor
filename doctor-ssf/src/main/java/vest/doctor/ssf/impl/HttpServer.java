package vest.doctor.ssf.impl;

import vest.doctor.sleipnir.Configuration;
import vest.doctor.sleipnir.Server;

import java.io.Closeable;

public class HttpServer implements Closeable {
    private final Configuration conf;
    private final HttpConfiguration httpConfiguration;
    private final Server server;

    public HttpServer(Configuration configuration, HttpConfiguration httpConfiguration) {
        this.conf = configuration;
        this.httpConfiguration = httpConfiguration;
        this.server = Server.start(configuration);
    }

    @Override
    public void close() {
        server.stop();
    }
}
