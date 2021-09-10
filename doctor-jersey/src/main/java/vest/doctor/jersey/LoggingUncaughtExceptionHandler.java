package vest.doctor.jersey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static final LoggingUncaughtExceptionHandler INSTANCE = new LoggingUncaughtExceptionHandler();

    private static final Logger log = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("uncaught exception from {}", t, e);
    }
}
