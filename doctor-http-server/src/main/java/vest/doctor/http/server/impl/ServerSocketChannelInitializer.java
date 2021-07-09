package vest.doctor.http.server.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public abstract class ServerSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private ChannelHandler server;

    public ChannelHandler getServer() {
        return server;
    }

    public void setServer(ChannelHandler server) {
        this.server = server;
    }
}
