package vest.doctor;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class CustomThreadFactory implements ThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory {

    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final boolean daemonize;
    private final String threadPrefix;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final ClassLoader classLoader;

    public CustomThreadFactory(boolean daemonize, String threadPrefix, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, ClassLoader classLoader) {
        this.daemonize = daemonize;
        this.threadPrefix = threadPrefix;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    @Override
    public Thread newThread(Runnable r) {
        return configure(defaultThreadFactory.newThread(r));
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return configure(ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool));
    }

    private <T extends Thread> T configure(T thread) {
        thread.setDaemon(daemonize);
        thread.setName(threadPrefix + counter.incrementAndGet());
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        thread.setContextClassLoader(classLoader);
        return thread;
    }
}
