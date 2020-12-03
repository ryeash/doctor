package doctor.cluster;

import doctor.cluster.client.Client;
import doctor.cluster.model.Direction;
import doctor.cluster.model.MessageContainer;
import doctor.cluster.model.MessageType;
import doctor.cluster.server.Server;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cluster {

    private final Client client;
    private final Server server;
    private final ExecutorService background;

    public Cluster(String host, int port, List<String> others) {
        this.server = new Server(host, port, this::receive);
        this.client = new Client(this::receive);
        this.background = Executors.newFixedThreadPool(32);

        if (others != null) {
            for (String other : others) {
                String[] split = other.split(":", 2);
                String seedHost = split[0].trim();
                int seedPort = Integer.parseInt(split[1].trim());
                client.send(new InetSocketAddress(seedHost, seedPort), newMessage(MessageType.JOINING));
            }
        }
    }

    private Client client() {
        return client;
    }

    private void receive(ChannelHandlerContext ctx, MessageContainer message) {

    }

    private static MessageContainer newMessage(MessageType type) {
        MessageContainer msg = new MessageContainer();
        msg.setType(type);
        msg.setDirection(Direction.RESPONSE);
        msg.setId(-1);
        msg.setData(MessageContainer.EMPTY_BYTES);
        msg.setMessageSize(0);
        return msg;
    }

}
