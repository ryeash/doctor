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
 * There are two executors provided automatically, their names are {@link BuiltInAppLoader#DEFAULT_EXECUTOR_NAME} and {@link BuiltInAppLoader#DEFAULT_SCHEDULED_EXECUTOR_NAME}
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

    private final int minThreads;
    private final int maxThreads;
    private final int keepAliveSeconds;
    private final ThreadPoolType type;
    private final String qualifier;
    private final List<Class<?>> providedTypes;
    private final CustomThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedExecutionHandler;

    public ConfigurationDrivenExecutorServiceProvider(ProviderRegistry providerRegistry, String name, ThreadPoolType forceType) {
        ConfigurationFacade configurationFacade = providerRegistry.configuration();
        String propertyPrefix = "executors." + name;
        this.qualifier = name;
        if (forceType != null) {
            this.type = forceType;
        } else {
            this.type = configurationFacade.get(propertyPrefix + ".type", ThreadPoolType.forkjoin, ThreadPoolType::valueOf);
        }
        this.providedTypes = switch (type) {
            case cached, fixed -> List.of(Executor.class, ExecutorService.class);
            case scheduled -> List.of(Executor.class, ExecutorService.class, ScheduledExecutorService.class);
            case forkjoin -> List.of(Executor.class, ExecutorService.class, ForkJoinPool.class);
        };

        minThreads = configurationFacade.get(propertyPrefix + ".minThreads", DEFAULT_MIN_THREADS, Integer::valueOf);
        maxThreads = configurationFacade.get(propertyPrefix + ".maxThreads", DEFAULT_MAX_THREADS, Integer::valueOf);
        if (minThreads < 0) {
            throw new IllegalArgumentException("invalid minThreads for executor " + name + ": must be greater than 0");
        }
        if (maxThreads < minThreads) {
            throw new IllegalArgumentException("invalid maxThreads for executor " + name + ": must be greater than minThreads");
        }
        keepAliveSeconds = configurationFacade.get(propertyPrefix + ".keepAliveSeconds", DEFAULT_KEEP_ALIVE, Integer::valueOf);
        if (keepAliveSeconds <= 0) {
            throw new IllegalArgumentException("invalid keepAliveSecond for executor " + name + ": must be greater than 0");
        }

        String uncaughtExceptionHandlerQualifier = configurationFacade.get(propertyPrefix + ".uncaughtExceptionHandler");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = providerRegistry.getProviderOpt(Thread.UncaughtExceptionHandler.class, uncaughtExceptionHandlerQualifier)
                .map(Provider::get)
                .orElse(LoggingUncaughtExceptionHandler.INSTANCE);

        this.threadFactory = new CustomThreadFactory(
                configurationFacade.get(propertyPrefix + ".daemonize", true, Boolean::valueOf),
                configurationFacade.get(propertyPrefix + ".nameFormat", name + "-%d"),
                uncaughtExceptionHandler,
                null);

        String rejectedExecutionHandlerQualifier = configurationFacade.get(propertyPrefix + ".rejectedExecutionHandler", "!!noValue!!");

        this.rejectedExecutionHandler = providerRegistry.getProviderOpt(RejectedExecutionHandler.class, rejectedExecutionHandlerQualifier)
                .map(DoctorProvider::get)
                .orElseGet(() ->
                        switch (RejectedExecutionType.valueOrDefault(rejectedExecutionHandlerQualifier)) {
                            case discard -> new ThreadPoolExecutor.DiscardPolicy();
                            case discardOldest -> new ThreadPoolExecutor.DiscardOldestPolicy();
                            case callerRuns -> new ThreadPoolExecutor.CallerRunsPolicy();
                            default -> new ThreadPoolExecutor.AbortPolicy();
                        });
    }

    @Override
    public Class<ExecutorService> type() {
        return ExecutorService.class;
    }

    @Override
    public String qualifier() {
        return qualifier;
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
    public ExecutorService get() {
        switch (type) {
            case fixed:
                ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                        minThreads,
                        maxThreads,
                        keepAliveSeconds,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(),
                        threadFactory,
                        rejectedExecutionHandler);
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
                        threadFactory,
                        rejectedExecutionHandler);
                cached.allowCoreThreadTimeOut(true);
                cached.prestartCoreThread();
                return Executors.unconfigurableExecutorService(cached);

            case scheduled:
                ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                        minThreads,
                        threadFactory,
                        rejectedExecutionHandler);
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
                        threadFactory,
                        threadFactory.getUncaughtExceptionHandler(),
                        true);
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }
    }
}
