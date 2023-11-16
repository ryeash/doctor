package vest.doctor.ssf.impl;

import vest.doctor.ssf.ExceptionHandler;
import vest.doctor.ssf.Handler;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

public record Configuration(
        String bindHost,
        int bindPort,
        int uriMaxLength,
        int headerMaxLength,
        int bodyMaxLength,
        int readBufferSize,
        int initialParseBufferSize,
        boolean directBuffers,
        Handler handler,
        ExceptionHandler exceptionHandler,
        ExecutorService executor) {

    public ByteBuffer allocateBuffer(int size) {
        return directBuffers ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }
}
