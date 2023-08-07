package vest.doctor.grpc;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Useful tools when working with gRPC server endpoints.
 */
public final class GrpcUtils {
    private GrpcUtils() {
    }

    /**
     * Adapt the {@link ListenableFuture} into a {@link CompletableFuture}, listening on a thread from the {@link ForkJoinPool#commonPool() common pool}.
     *
     * @param listenableFuture the listenable future to adapt
     * @return a completable future adapted from the listenable future
     */
    public static <T> CompletableFuture<T> listen(ListenableFuture<T> listenableFuture) {
        return listen(listenableFuture, ForkJoinPool.commonPool());
    }

    /**
     * Adapt the {@link ListenableFuture} into a {@link CompletableFuture}, listening on a thread from the given {@link java.util.concurrent.Executor}.
     *
     * @param listenableFuture the listenable future to adapt
     * @param executor         the executor to use in {@link ListenableFuture#addListener(Runnable, Executor)}
     * @return a completable future adapted from the listenable future
     */
    public static <T> CompletableFuture<T> listen(ListenableFuture<T> listenableFuture, Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        listenableFuture.addListener(new ListenerToFutureBridge<>(listenableFuture, future), executor);
        return future;
    }

    record ListenerToFutureBridge<T>(ListenableFuture<T> listenableFuture,
                                     CompletableFuture<T> future) implements Runnable {
        @Override
        public void run() {
            try {
                if (listenableFuture.isCancelled()) {
                    future.cancel(true);
                } else {
                    future.complete(listenableFuture.get());
                }
            } catch (ExecutionException ee) {
                if (ee.getCause() != null) {
                    future.completeExceptionally(ee.getCause());
                } else {
                    future.completeExceptionally(ee);
                }
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }
    }
}
