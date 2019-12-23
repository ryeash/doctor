package vest.doctor;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class ConfigurationDrivenExecutorServiceProvider implements DoctorProvider<ExecutorService> {

    public enum ThreadPoolType {
        threadpool, scheduled, forkjoin
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

    private final ConfigurationFacade configurationFacade;
    private final String propertyPrefix;
    private final ThreadPoolType type;
    private final String qualifier;
    private final List<Class<?>> providedTypes;
    private final ExecutorBuilder builder;

    public ConfigurationDrivenExecutorServiceProvider(BeanProvider beanProvider, String name, ThreadPoolType forceType) {
        this.configurationFacade = beanProvider.configuration();
        this.propertyPrefix = "executors." + name + ".";
        this.qualifier = name;
        if (forceType != null) {
            this.type = forceType;
        } else {
            this.type = configurationFacade.get(propertyPrefix + ".type", ThreadPoolType.threadpool, ThreadPoolType::valueOf);
        }
        switch (type) {
            case threadpool:
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

        this.builder = ExecutorBuilder.newExecutor()
                .setDaemonize(configurationFacade.get(propertyPrefix + ".daemonize", true, Boolean::valueOf))
                .setThreadNamePrefix(configurationFacade.get(propertyPrefix + ".threadPrefix", propertyPrefix));

        String uncaughtExceptionHandlerQualifier = configurationFacade.get(propertyPrefix + ".uncaughtExceptionHandler");
        beanProvider.getProviderOpt(Thread.UncaughtExceptionHandler.class, uncaughtExceptionHandlerQualifier)
                .map(Provider::get)
                .ifPresent(builder::setUncaughtExceptionHandler);

        String rejectedExecutionHandlerQualifier = configurationFacade.get(propertyPrefix + ".rejectedExecutionHandler", "!!noValue!!");

        RejectedExecutionHandler rejectedExecutionHandler = beanProvider.getProviderOpt(RejectedExecutionHandler.class, rejectedExecutionHandlerQualifier)
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
        builder.setRejectedExecutionHandler(rejectedExecutionHandler);
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
    public void validateDependencies(BeanProvider beanProvider) {
    }

    @Override
    public ExecutorService get() {
        switch (type) {
            case threadpool:
                int maxQueueSize = configurationFacade.get(propertyPrefix + ".maxQueueSize", -1, Integer::valueOf);
                BlockingQueue<Runnable> workQueue;
                if (maxQueueSize < 0) {
                    workQueue = new LinkedBlockingQueue<>();
                } else if (maxQueueSize == 0) {
                    workQueue = new SynchronousQueue<>();
                } else {
                    workQueue = new ArrayBlockingQueue<>(maxQueueSize);
                }
                return builder.threadPoolExecutor(
                        configurationFacade.get(propertyPrefix + ".minThreads", 1, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".maxThreads", Runtime.getRuntime().availableProcessors() * 2, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".keepAliveSeconds", 600, Integer::valueOf),
                        workQueue);
            case scheduled:
                return builder.scheduledExecutor(
                        configurationFacade.get(propertyPrefix + ".minThreads", 1, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".maxThreads", Runtime.getRuntime().availableProcessors() * 2, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".keepAliveSeconds", 600, Integer::valueOf));
            case forkjoin:
                return builder.forkJoinPool(
                        configurationFacade.get(propertyPrefix + ".parallelism", 1, Integer::valueOf));
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }
    }
}
