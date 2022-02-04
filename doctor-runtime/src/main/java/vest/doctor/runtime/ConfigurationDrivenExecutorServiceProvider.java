package vest.doctor.runtime;

import jakarta.inject.Provider;
import vest.doctor.ConfigurationFacade;
import vest.doctor.CustomThreadFactory;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provider implementation that provides instances of {@link ExecutorService}s based on configuration properties.
 * <p>
 * Supported properties:
 * executors.[name].type - the type of the executor - allowed value is one of {@link ThreadPoolType}
 * executors.[name].minThreads - the minimum number of threads in the pool; not valid for forkjoin type
 * executors.[name].maxThreads - the maximum number of threads in the pool; not valid for cached type
 * executors.[name].keepAliveSeconds - the number of seconds to keep idle threads in the pool alive before allowing them to be destroyed; not valid for forkjoin type
 * executors.[name].uncaughtExceptionHandler - the qualifier for a provided {@link java.lang.Thread.UncaughtExceptionHandler} to use in the thread pool
 * executors.[name].daemonize - sets whether threads created by the executor will be daemons; {@link Thread#setDaemon(boolean)}
 * executors.[name].nameFormat - format of the thread names; e.g. "background-%d"
 * executors.[name].rejectedExecutionHandler - either the qualifier for a provided {@link RejectedExecutionHandler} or the name of one of the built in handlers: discard, discardOldest, callerRuns, abort
 * <p>
 * There are two executors provided automatically, their names are {@link BuiltInApplicationLoader#DEFAULT_EXECUTOR_NAME} and {@link BuiltInApplicationLoader#DEFAULT_SCHEDULED_EXECUTOR_NAME}
 */
public class ConfigurationDrivenExecutorServiceProvider implements DoctorProvider<ExecutorService> {

    public static final int DEFAULT_MIN_THREADS = 1;
    public static final int DEFAULT_MAX_THREADS = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    public static final int DEFAULT_KEEP_ALIVE = 60;

    public enum ThreadPoolType {
        cached, fixed, scheduled, forkjoin
    }

    public enum RejectedExecutionType {
        discard, discardOldest, callerRuns, abort;

        public static RejectedExecutionType valueOrDefault(String string) {
            for (RejectedExecutionType value : RejectedExecutionType.values()) {
                if (value.name().equals(string)) {
                    return value;
                }
            }
            return abort;
        }
    }

    private final ProviderRegistry providerRegistry;
    private final ConfigurationFacade executorConfig;
    private final String name;
    private final ThreadPoolType type;
    private final List<Class<?>> providedTypes;

    public ConfigurationDrivenExecutorServiceProvider(ProviderRegistry providerRegistry, String name, ThreadPoolType forceType) {
        this.providerRegistry = providerRegistry;
        this.executorConfig = providerRegistry.configuration().subsection("executors." + name + ".");
        this.name = name;
        if (forceType != null) {
            this.type = forceType;
        } else {
            this.type = executorConfig.get("type", ThreadPoolType.forkjoin, ThreadPoolType::valueOf);
        }
        this.providedTypes = switch (type) {
            case cached, fixed -> List.of(Executor.class, ExecutorService.class);
            case scheduled -> List.of(Executor.class, ExecutorService.class, ScheduledExecutorService.class);
            case forkjoin -> List.of(Executor.class, ExecutorService.class, ForkJoinPool.class);
        };
    }

    @Override
    public Class<ExecutorService> type() {
        return ExecutorService.class;
    }

    @Override
    public String qualifier() {
        return name;
    }

    @Override
    public Class<? extends Annotation> scope() {
        return null;
    }

    @Override
    public List<Class<?>> allProvidedTypes() {
        return providedTypes;
    }

    @Override
    public void validateDependencies(ProviderRegistry providerRegistry) {
    }

    @Override
    public void destroy(ExecutorService instance) {
    }

    @Override
    public ExecutorService get() {
        int minThreads = executorConfig.get("minThreads", DEFAULT_MIN_THREADS, Integer::valueOf);
        int maxThreads = executorConfig.get("maxThreads", DEFAULT_MAX_THREADS, Integer::valueOf);
        if (minThreads < 0) {
            throw new IllegalArgumentException("invalid minThreads for executor " + name + ": must be greater than 0");
        }
        if (maxThreads < minThreads) {
            throw new IllegalArgumentException("invalid maxThreads for executor " + name + ": must be greater than minThreads");
        }
        int keepAliveSeconds = executorConfig.get("keepAliveSeconds", DEFAULT_KEEP_ALIVE, Integer::valueOf);
        if (keepAliveSeconds <= 0) {
            throw new IllegalArgumentException("invalid keepAliveSecond for executor " + name + ": must be greater than 0");
        }
        switch (type) {
            case fixed:
                ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                        minThreads,
                        maxThreads,
                        keepAliveSeconds,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(),
                        getThreadFactory(),
                        getRejectedExecutionHandler());
                threadPoolExecutor.allowCoreThreadTimeOut(false);
                threadPoolExecutor.prestartAllCoreThreads();
                return Executors.unconfigurableExecutorService(threadPoolExecutor);

            case cached:
                ThreadPoolExecutor cached = new ThreadPoolExecutor(
                        minThreads,
                        Integer.MAX_VALUE,
                        keepAliveSeconds,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        getThreadFactory(),
                        getRejectedExecutionHandler());
                cached.allowCoreThreadTimeOut(true);
                cached.prestartCoreThread();
                return Executors.unconfigurableExecutorService(cached);

            case scheduled:
                ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                        minThreads,
                        getThreadFactory(),
                        getRejectedExecutionHandler());
                scheduledThreadPoolExecutor.setMaximumPoolSize(maxThreads);
                scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
                scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);
                scheduledThreadPoolExecutor.prestartAllCoreThreads();
                scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
                scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
                return Executors.unconfigurableScheduledExecutorService(scheduledThreadPoolExecutor);

            case forkjoin:
                return new ForkJoinPool(
                        maxThreads,
                        getThreadFactory(),
                        getThreadFactory().getUncaughtExceptionHandler(),
                        true);
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }
    }

    @Override
    public void close() {
    }

    private CustomThreadFactory getThreadFactory() {
        String uncaughtExceptionHandlerQualifier = executorConfig.get("uncaughtExceptionHandler");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = providerRegistry.getProviderOpt(Thread.UncaughtExceptionHandler.class, uncaughtExceptionHandlerQualifier)
                .map(Provider::get)
                .orElse(LoggingUncaughtExceptionHandler.INSTANCE);
        return new CustomThreadFactory(
                executorConfig.get("daemonize", true, Boolean::valueOf),
                executorConfig.get("nameFormat", name + "-%d"),
                uncaughtExceptionHandler,
                null);
    }

    private RejectedExecutionHandler getRejectedExecutionHandler() {
        String rejectedExecutionHandlerQualifier = executorConfig.get("rejectedExecutionHandler", "!!noValue!!");
        return providerRegistry.getProviderOpt(RejectedExecutionHandler.class, rejectedExecutionHandlerQualifier)
                .map(DoctorProvider::get)
                .orElseGet(() ->
                        switch (RejectedExecutionType.valueOrDefault(rejectedExecutionHandlerQualifier)) {
                            case discard -> new ThreadPoolExecutor.DiscardPolicy();
                            case discardOldest -> new ThreadPoolExecutor.DiscardOldestPolicy();
                            case callerRuns -> new ThreadPoolExecutor.CallerRunsPolicy();
                            default -> new ThreadPoolExecutor.AbortPolicy();
                        });
    }
}
