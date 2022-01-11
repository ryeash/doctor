package vest.doctor.atomic;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.util.Try;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class ExclusiveManagedResource<R> implements ManagedResource<R> {
    private final ManagedLock lock = new ManagedLock();
    private final R resource;

    ExclusiveManagedResource(R resource) {
        this.resource = Objects.requireNonNull(resource);
    }

    @Override
    public <V> Try<V> borrow(ThrowingFunction<R, V> function) {
        return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, function);
    }

    @Override
    public <V> Try<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function) {
        return lock.withLock(acquireTimeout, unit, resource, function).accept(v -> {
            if (v == resource) {
                throw new UnsupportedOperationException("the resource may not be returned by the function");
            }
        });
    }

    @Override
    public Try<Void> borrow(ThrowingConsumer<R> action) {
        return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, action);
    }

    @Override
    public Try<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action) {
        return lock.withLock(acquireTimeout, unit, resource, action);
    }
}
