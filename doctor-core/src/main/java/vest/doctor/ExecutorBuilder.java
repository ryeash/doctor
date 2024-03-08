package vest.doctor;

import vest.doctor.runtime.LoggingUncaughtExceptionHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Builder to create {@link ExecutorService} classes. Supports creating
 * {@link ExecutorService}, {@link ScheduledExecutorService}, and {@link ForkJoinPool} executors.
 * <p>
 * Expected usage:
 * <code><pre>
 * <literal>@</literal>Factory
 * <literal>@</literal>Singleton
 * <literal>@</literal>Named("background")
 * public ScheduledExecutorService scheduledExecutorService() {
 *     return ExecutorBuilder.start()
 *             .setMinThreads(8)
 *             .setMaxThreads(8)
 *             .scheduled();
 * }
 * </pre></code>
 */
public class ExecutorBuilder {

    /**
     * Start building an executor.
     */
    public static ExecutorBuilder start() {
        return new ExecutorBuilder();
    }

    private int minThreads = 1;
    private int maxThreads = 8;
    private int keepAliveSeconds = 60;
    private boolean allowCoreThreadTimeOut = false;
    private Supplier<BlockingQueue<Runnable>> blockingQueueBuilder = LinkedBlockingQueue::new;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = LoggingUncaughtExceptionHandler.INSTANCE;
    private boolean daemonize = true;
    private String threadNameFormat = "thread-%d";
    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
    private ClassLoader classLoader = null;
    private boolean asyncMode = false;

    private ExecutorBuilder() {
    }

    /**
     * Set the minimum threads for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}
     *
     * @default 1
     * @see ThreadPoolExecutor#setCorePoolSize(int)
     */
    public ExecutorBuilder setMinThreads(int minThreads) {
        this.minThreads = minThreads;
        return this;
    }

    /**
     * Set the maximum threads for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}, {@link #forkjoin()}
     *
     * @default 8
     * @see ThreadPoolExecutor#setMaximumPoolSize(int)
     */
    public ExecutorBuilder setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    /**
     * Set the keepAlive timeout seconds for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}
     *
     * @default 60
     * @see ThreadPoolExecutor#setKeepAliveTime(long, TimeUnit)
     */
    public ExecutorBuilder setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
        return this;
    }

    /**
     * Set the allowCoreThreadTimeOut for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}
     *
     * @default false
     * @see ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
     */
    public ExecutorBuilder setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
        return this;
    }

    /**
     * Set the blockingQueueBuilder for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}
     *
     * @default {@link LinkedBlockingQueue}
     * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue)
     */
    public ExecutorBuilder setBlockingQueueBuilder(Supplier<BlockingQueue<Runnable>> blockingQueueBuilder) {
        this.blockingQueueBuilder = blockingQueueBuilder;
        return this;
    }

    /**
     * Set the {@link java.lang.Thread.UncaughtExceptionHandler} for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}, {@link #forkjoin()}
     *
     * @default {@link LoggingUncaughtExceptionHandler}
     */
    public ExecutorBuilder setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * Set whether to create the threads in daemon mode.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}, {@link #forkjoin()}
     *
     * @default true
     * @see Thread#setDaemon(boolean) 
     */
    public ExecutorBuilder setDaemonize(boolean daemonize) {
        this.daemonize = daemonize;
        return this;
    }

    /**
     * Set the thread name format for threads created by the executor service.
     * Uses {@link String#format(String, Object...)} to format the thread name with
     * a monotonically incrementing thread number.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}, {@link #forkjoin()}
     *
     * @default thread-%d
     * @see Thread#setName(String)
     */
    public ExecutorBuilder setThreadNameFormat(String threadNameFormat) {
        this.threadNameFormat = threadNameFormat;
        return this;
    }

    /**
     * Set the {@link RejectedExecutionHandler} for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}
     *
     * @default {@link ThreadPoolExecutor.AbortPolicy}
     * @see ThreadPoolExecutor#setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public ExecutorBuilder setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        return this;
    }

    /**
     * Set the {@link ClassLoader} for the executor service.
     * <p>
     * Applies to: {@link #standard()}, {@link #scheduled()}, {@link #forkjoin()}
     *
     * @default null (will use the system classloader)
     * @see Thread#setContextClassLoader(ClassLoader)
     */
    public ExecutorBuilder setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Set the async mode for the executor service.
     * <p>
     * Applies to: {@link #forkjoin()}
     *
     * @default false
     * @see ForkJoinPool#getAsyncMode()
     */
    public ExecutorBuilder setAsyncMode(boolean asyncMode) {
        this.asyncMode = asyncMode;
        return this;
    }

    /**
     * Create an {@link ExecutorService} similar to {@link Executors#newFixedThreadPool(int, ThreadFactory)}.
     *
     * @return a new executor service
     */
    public ExecutorService standard() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                minThreads,
                maxThreads,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                blockingQueueBuilder.get(),
                getThreadFactory(),
                rejectedExecutionHandler);
        threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        threadPoolExecutor.prestartCoreThread();
        return Executors.unconfigurableExecutorService(threadPoolExecutor);
    }

    /**
     * Create an {@link ScheduledExecutorService} similar to {@link Executors#newScheduledThreadPool(int, ThreadFactory)}
     *
     * @return a new scheduled executor service
     */
    public ScheduledExecutorService scheduled() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                minThreads,
                getThreadFactory(),
                rejectedExecutionHandler);
        scheduledThreadPoolExecutor.setMaximumPoolSize(maxThreads);
        scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        scheduledThreadPoolExecutor.prestartAllCoreThreads();
        scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        return Executors.unconfigurableScheduledExecutorService(scheduledThreadPoolExecutor);
    }

    /**
     * Create a new {@link ForkJoinPool}.
     *
     * @return a new fork-join pool
     */
    public ForkJoinPool forkjoin() {
        return new ForkJoinPool(
                maxThreads,
                getThreadFactory(),
                uncaughtExceptionHandler,
                asyncMode);
    }

    private CustomThreadFactory getThreadFactory() {
        return new CustomThreadFactory(
                daemonize,
                threadNameFormat,
                uncaughtExceptionHandler,
                classLoader);
    }
}
