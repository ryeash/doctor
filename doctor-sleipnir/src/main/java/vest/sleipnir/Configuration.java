package vest.sleipnir;

import java.nio.ByteBuffer;

public record Configuration(String bindHost,
                            int bindPort,
                            int readBufferSize,
                            int writeBufferSize,
                            boolean directBuffers,
                            SocketInitializer socketInitializer) {

    public ByteBuffer allocateBuffer(int size) {
        return directBuffers ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }
}
