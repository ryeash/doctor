package vest.doctor.netty;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.CompletableFuture;

public final class CompletableFutureListener<T> implements GenericFutureListener<Future<T>> {

    private final CompletableFuture<T> future;

    public CompletableFutureListener(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void operationComplete(Future<T> future) {
        if (future.isSuccess()) {
            this.future.complete(null);
        } else {
            this.future.completeExceptionally(future.cause());
        }
    }
}
