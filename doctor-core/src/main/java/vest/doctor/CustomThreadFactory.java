package vest.doctor;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements {@link ThreadFactory} and {@link java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory} and allows
 * for customizing the generated {@link Thread}s.
 */
public final class CustomThreadFactory implements ThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory {

    private static final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final boolean daemonize;
    private final String nameFormat;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private final ClassLoader classLoader;

    /**
     * Create a new thread factory.
     *
     * @param daemonize                whether the created threads will be deamons; see {@link Thread#setDaemon(boolean)}
     * @param nameFormat               the name format to use for the threads; Example "background-%d";
     *                                 see {@link String#format(String, Object...)} and {@link Thread#setName(String)}
     * @param uncaughtExceptionHandler the {@link java.lang.Thread.UncaughtExceptionHandler} to use for the threads;
     *                                 see {@link Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}
     * @param classLoader              the context {@link ClassLoader} to use for the threads; see {@link Thread#setContextClassLoader(ClassLoader)}
     */
    public CustomThreadFactory(boolean daemonize, String nameFormat, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, ClassLoader classLoader) {
        this.daemonize = daemonize;
        this.nameFormat = nameFormat;
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
        thread.setName(String.format(nameFormat, counter.incrementAndGet()));
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        thread.setContextClassLoader(classLoader);
        return thread;
    }
}
