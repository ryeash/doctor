package vest.doctor.flow;

import vest.doctor.util.Try;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An {@link ExecutorService} that invokes all tasks in the caller thread.
 */
public class CallerRunsExecutorService implements ExecutorService {

    private static final class CallerRunsExecutorServiceHolder {
        private static final ExecutorService INSTANCE = new CallerRunsExecutorService();
    }

    public static ExecutorService instance() {
        return CallerRunsExecutorServiceHolder.INSTANCE;
    }

    private boolean shutdown = false;

    private CallerRunsExecutorService() {
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return shutdown;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return Try.call(task).toCompletableFuture();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return Try.run(task::run).apply(v -> result).toCompletableFuture();
    }

    @Override
    public Future<?> submit(Runnable task) {
        return Try.run(task::run).toCompletableFuture();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
                .map(c -> Try.call(c).toCompletableFuture())
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        return tasks.stream()
                .map(c -> Try.call(c).toCompletableFuture())
                .collect(Collectors.toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException {
        for (Callable<T> task : tasks) {
            try {
                return task.call();
            } catch (Exception e) {
                // ignored
            }
        }
        throw new ExecutionException("no task succeeded", new IllegalStateException());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException {
        for (Callable<T> task : tasks) {
            try {
                return task.call();
            } catch (Exception e) {
                // ignored
            }
        }
        throw new ExecutionException("no task succeeded", new IllegalStateException());
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
