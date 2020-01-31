package vest.doctor;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
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

public class ConfigurationDrivenExecutorServiceProvider implements DoctorProvider<ExecutorService> {

    public static final int DEFAULT_MIN_THREADS = 1;
    public static final int DEFAULT_MAX_THREADS = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    public static final int DEFAULT_KEEP_ALIVE = 600;

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
        String propertyPrefix = "executors." + name + ".";
        this.qualifier = name;
        if (forceType != null) {
            this.type = forceType;
        } else {
            this.type = configurationFacade.get(propertyPrefix + ".type", ThreadPoolType.fixed, ThreadPoolType::valueOf);
        }
        switch (type) {
            case cached:
            case fixed:
                this.providedTypes = Arrays.asList(Executor.class, ExecutorService.class);
                break;
            case scheduled:
                this.providedTypes = Arrays.asList(Executor.class, ExecutorService.class, ScheduledExecutorService.class);
                break;
            case forkjoin:
                this.providedTypes = Arrays.asList(Executor.class, ExecutorService.class, ForkJoinPool.class);
                break;
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }

        minThreads = configurationFacade.get(propertyPrefix + ".minThreads", DEFAULT_MIN_THREADS, Integer::valueOf);
        maxThreads = configurationFacade.get(propertyPrefix + ".maxThreads", DEFAULT_MAX_THREADS, Integer::valueOf);
        keepAliveSeconds = configurationFacade.get(propertyPrefix + ".keepAliveSeconds", DEFAULT_KEEP_ALIVE, Integer::valueOf);

        String uncaughtExceptionHandlerQualifier = configurationFacade.get(propertyPrefix + ".uncaughtExceptionHandler");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = providerRegistry.getProviderOpt(Thread.UncaughtExceptionHandler.class, uncaughtExceptionHandlerQualifier)
                .map(Provider::get)
                .orElse(Thread.getDefaultUncaughtExceptionHandler());

        threadFactory = new CustomThreadFactory(
                configurationFacade.get(propertyPrefix + ".daemonize", true, Boolean::valueOf),
                configurationFacade.get(propertyPrefix + ".threadPrefix", propertyPrefix),
                uncaughtExceptionHandler,
                null);

        String rejectedExecutionHandlerQualifier = configurationFacade.get(propertyPrefix + ".rejectedExecutionHandler", "!!noValue!!");

        this.rejectedExecutionHandler = providerRegistry.getProviderOpt(RejectedExecutionHandler.class, rejectedExecutionHandlerQualifier)
                .map(DoctorProvider::get)
                .orElseGet(() -> {
                    switch (RejectedExecutionType.valueOrDefault(rejectedExecutionHandlerQualifier)) {
                        case discard:
                            return new ThreadPoolExecutor.DiscardPolicy();
                        case discardOldest:
                            return new ThreadPoolExecutor.DiscardOldestPolicy();
                        case callerRuns:
                            return new ThreadPoolExecutor.CallerRunsPolicy();
                        case abort:
                        default:
                            return new ThreadPoolExecutor.AbortPolicy();
                    }
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
                        Integer.MAX_VALUE,
                        TimeUnit.DAYS,
                        new LinkedBlockingQueue<>(),
                        threadFactory,
                        rejectedExecutionHandler);
                threadPoolExecutor.allowCoreThreadTimeOut(false);
                threadPoolExecutor.prestartAllCoreThreads();
                return Executors.unconfigurableExecutorService(threadPoolExecutor);

            case cached:
                ThreadPoolExecutor cached = new ThreadPoolExecutor(
                        minThreads,
                        maxThreads,
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
