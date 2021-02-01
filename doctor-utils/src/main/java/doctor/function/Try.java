package doctor.function;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public final class Try<T> {

    public static CompletableFuture<Void> run(ThrowingRunnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static CompletableFuture<Void> runAsync(ThrowingRunnable runnable) {
        return runAsync(runnable, ForkJoinPool.commonPool());
    }

    public static CompletableFuture<Void> runAsync(ThrowingRunnable runnable, ExecutorService executorService) {
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            executorService.submit(() -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <V> CompletableFuture<V> supply(ThrowingSupplier<V> action) {
        try {
            return success(action.get());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <V> CompletableFuture<V> supplyAsync(ThrowingSupplier<V> action) {
        return supplyAsync(action, ForkJoinPool.commonPool());
    }

    public static <V> CompletableFuture<V> supplyAsync(ThrowingSupplier<V> action, ExecutorService executorService) {
        try {
            CompletableFuture<V> future = new CompletableFuture<>();
            executorService.submit(() -> {
                try {
                    future.complete(action.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <I, V> CompletableFuture<V> apply(I input, ThrowingFunction<I, V> mapper) {
        try {
            return success(mapper.apply(input));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <I, V> CompletableFuture<V> applyAsync(I input, ThrowingFunction<I, V> mapper) {
        return applyAsync(input, mapper, ForkJoinPool.commonPool());
    }

    public static <I, V> CompletableFuture<V> applyAsync(I input, ThrowingFunction<I, V> mapper, ExecutorService executorService) {
        try {
            CompletableFuture<V> future = new CompletableFuture<>();
            executorService.submit(() -> {
                try {
                    future.complete(mapper.apply(input));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <V> CompletableFuture<V> success(V value) {
        return CompletableFuture.completedFuture(value);
    }

    public static <V> CompletableFuture<V> failure(Throwable exception) {
        CompletableFuture<V> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
}
