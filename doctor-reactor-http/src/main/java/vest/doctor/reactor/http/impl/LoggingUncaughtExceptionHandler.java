package vest.doctor.reactor.http.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    public static final Thread.UncaughtExceptionHandler INSTANCE = new LoggingUncaughtExceptionHandler();

    private final static Logger log = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("task execution in background thread {} failed with exception", t, e);
    }
}
