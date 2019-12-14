package vest.doctor;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    private final BeanProvider beanProvider;
    private final ConfigurationFacade configurationFacade;
    private final String propertyPrefix;
    private final String type;
    private final String qualifier;
    private final List<Class<?>> providedTypes;

    public ConfigurationDrivenExecutorServiceProvider(BeanProvider beanProvider, String name, String forceType) {
        this.beanProvider = beanProvider;
        this.configurationFacade = beanProvider.configuration();
        this.propertyPrefix = "executors." + name + ".";
        this.qualifier = name;
        if (forceType != null) {
            this.type = forceType;
        } else {
            this.type = configurationFacade.get(propertyPrefix + ".type", "threadpool");
        }
        switch (type) {
            case "threadpool":
                this.providedTypes = Arrays.asList(ExecutorService.class, Executor.class);
                break;
            case "scheduled":
                this.providedTypes = Arrays.asList(ExecutorService.class, Executor.class, ScheduledExecutorService.class);
                break;
            case "forkjoin":
                this.providedTypes = Arrays.asList(ExecutorService.class, Executor.class, ForkJoinPool.class);
                break;
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }
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
        ExecutorBuilder builder = ExecutorBuilder.newExecutor()
                .setDaemonize(configurationFacade.get(propertyPrefix + ".daemonize", true, Boolean::valueOf))
                .setThreadNamePrefix(configurationFacade.get(propertyPrefix + ".threadPrefix", propertyPrefix));

        String uncaughtExceptionHandlerQualifier = configurationFacade.get(propertyPrefix + ".uncaughtExceptionHandler");
        beanProvider.getProviderOpt(Thread.UncaughtExceptionHandler.class, uncaughtExceptionHandlerQualifier)
                .map(Provider::get)
                .ifPresent(builder::setUncaughtExceptionHandler);

        String rejectedExecutionHandlerQualifier = configurationFacade.get(propertyPrefix + ".rejectedExecutionHandler", "!!noValue!!");
        Optional<RejectedExecutionHandler> rehOpt = beanProvider.getProviderOpt(RejectedExecutionHandler.class, rejectedExecutionHandlerQualifier).map(DoctorProvider::get);
        if (rehOpt.isPresent()) {
            builder.setRejectedExecutionHandler(rehOpt.get());
        } else {
            switch (rejectedExecutionHandlerQualifier) {
                case "discard":
                    builder.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
                    break;
                case "discardOldest":
                    builder.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                    break;
                case "callerRuns":
                    builder.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    break;
                case "abort":
                default:
                    builder.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            }
        }

        switch (type) {
            case "threadpool":
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
            case "scheduled":
                return builder.scheduledExecutor(
                        configurationFacade.get(propertyPrefix + ".minThreads", 1, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".maxThreads", Runtime.getRuntime().availableProcessors() * 2, Integer::valueOf),
                        configurationFacade.get(propertyPrefix + ".keepAliveSeconds", 600, Integer::valueOf));
            case "forkjoin":
                return builder.forkJoinPool(
                        configurationFacade.get(propertyPrefix + ".parallelism", 1, Integer::valueOf));
            default:
                throw new IllegalArgumentException("unknown executor service type: " + type);
        }
    }
}
