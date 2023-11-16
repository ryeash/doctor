package vest.doctor.ssf;

import java.nio.ByteBuffer;

public interface ChannelWriter {
    boolean writeTo(ByteBuffer buf);
}
