package vest.doctor.sleipnir;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;

public record ChannelContext(Selector selector,
                             SocketChannel socketChannel,
                             UUID id,
                             Map<String, Object> attributes,
                             Flow.Publisher<ByteBuffer> dataInput,
                             Flow.Subscriber<ByteBuffer> dataOutput) {
}
