package vest.doctor.jaxrs;


import io.netty.channel.Channel;

public record JAXRSServerHandle(Channel channel) implements AutoCloseable {

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
    }
}
