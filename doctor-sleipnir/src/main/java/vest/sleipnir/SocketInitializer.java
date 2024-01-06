package vest.sleipnir;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public interface SocketInitializer {
    Flow.Publisher<ByteBuffer> initialize(Channel channel);
}
