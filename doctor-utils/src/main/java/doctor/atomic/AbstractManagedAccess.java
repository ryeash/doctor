package doctor.atomic;


import doctor.function.ThrowingConsumer;
import doctor.function.ThrowingFunction;
import doctor.function.ThrowingRunnable;
import doctor.function.ThrowingSupplier;
import doctor.function.Try;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractManagedAccess implements ManagedAccess {

    protected ExecutorService executorService;

    @Override
    public ManagedAccess async() {
        return async(ForkJoinPool.commonPool());
    }

    @Override
    public ManagedAccess async(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    @Override
    public ManagedAccess sync() {
        this.executorService = null;
        return this;
    }

    @Override
    public CompletableFuture<Void> guard(ThrowingRunnable runnable) {
        return guard(Long.MAX_VALUE, TimeUnit.MILLISECONDS, runnable);
    }

    @Override
    public CompletableFuture<Void> guard(long acquireTimeout, TimeUnit unit, ThrowingRunnable runnable) {
        return guard(acquireTimeout, unit, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <I, R> CompletableFuture<R> guard(I input, ThrowingFunction<I, R> function) {
        return guard(Long.MAX_VALUE, TimeUnit.MILLISECONDS, input, function);
    }

    @Override
    public <I, R> CompletableFuture<R> guard(long acquireTimeout, TimeUnit unit, I input, ThrowingFunction<I, R> function) {
        return guard(acquireTimeout, unit, () -> function.apply(input));
    }

    @Override
    public <R> CompletableFuture<R> guard(ThrowingSupplier<R> supplier) {
        return guard(Long.MAX_VALUE, TimeUnit.MILLISECONDS, supplier);
    }

    @Override
    public <R> CompletableFuture<R> guard(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier) {
        return guardIt(acquireTimeout, unit, supplier);
    }

    @Override
    public <I> CompletableFuture<Void> guard(I input, ThrowingConsumer<I> action) {
        return guard(Long.MAX_VALUE, TimeUnit.MILLISECONDS, input, action);
    }

    @Override
    public <I> CompletableFuture<Void> guard(long acquireTimeout, TimeUnit unit, I input, ThrowingConsumer<I> action) {
        return guard(acquireTimeout, unit, () -> action.accept(input));
    }

    private <R> CompletableFuture<R> guardIt(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier) {
        if (executorService != null) {
            return Try.supplyAsync(() -> execute(acquireTimeout, unit, supplier), executorService);
        } else {
            return Try.supply(() -> execute(acquireTimeout, unit, supplier));
        }
    }

    private <R> R execute(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier) throws Exception {
        acquire(acquireTimeout, unit);
        try {
            return supplier.get();
        } finally {
            release();
        }
    }

    protected abstract void acquire(long acquireTimeout, TimeUnit unit) throws TimeoutException, InterruptedException;

    protected abstract void release() throws Exception;
}
