package vest.doctor.atomic;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.Try;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class LimitedManagedResource<R> implements ManagedResource<R> {
    private final R resource;
    private final ManagedSemaphore semaphore;

    public LimitedManagedResource(int concurrentLimit, R resource) {
        if (concurrentLimit < 1) {
            throw new IllegalArgumentException("usage limit must be greater than zero");
        }
        this.resource = Objects.requireNonNull(resource);
        this.semaphore = new ManagedSemaphore(concurrentLimit);
    }

    @Override
    public <V> Try<V> borrow(ThrowingFunction<R, V> function) {
        return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, function);
    }

    @Override
    public <V> Try<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function) {
        return semaphore.withPermit(acquireTimeout, unit, resource, function).accept(v -> {
            if (v == resource) {
                throw new UnsupportedOperationException("the resource may not be returned by the function");
            }
        });
    }

    @Override
    public Try<Void> borrow(ThrowingConsumer<R> action) {
        return semaphore.withPermit(resource, action);
    }

    @Override
    public Try<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action) {
        return semaphore.withPermit(acquireTimeout, unit, resource, action);
    }
}
