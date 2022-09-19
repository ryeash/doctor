package vest.doctor.grpc;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class GrpcUtils {
    private GrpcUtils() {
    }

    public static <T> CompletableFuture<T> listen(ListenableFuture<T> listenableFuture) {
        return listen(listenableFuture, ForkJoinPool.commonPool());
    }

    public static <T> CompletableFuture<T> listen(ListenableFuture<T> listenableFuture, Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        listenableFuture.addListener(() -> {
            try {
                future.complete(listenableFuture.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, executor);
        return future;
    }
}
