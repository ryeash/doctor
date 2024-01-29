package vest.doctor.ssf.impl;

import vest.doctor.ssf.ExceptionHandler;
import vest.doctor.ssf.Handler;

import java.util.concurrent.ExecutorService;

public record HttpConfiguration(int uriMaxLength,
                                int bodyMaxLength,
                                int initialParseBufferSize,
                                int headerMaxLength,
                                Handler handler,
                                ExceptionHandler exceptionHandler,
                                ExecutorService workerThreadPool) {
}
