package vest.doctor.http.server.impl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import vest.doctor.http.server.HttpServer;

public abstract class ServerSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private HttpServer server;

    public HttpServer getServer() {
        return server;
    }

    public void setServer(HttpServer server) {
        this.server = server;
    }
}
