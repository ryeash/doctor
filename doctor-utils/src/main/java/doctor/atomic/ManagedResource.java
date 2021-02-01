package doctor.atomic;

import doctor.function.ThrowingConsumer;
import doctor.function.ThrowingFunction;
import doctor.function.Try;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controls access to a single instance of a managed resource. Implementations have
 * different guarantees about what happens during a borrow.
 *
 * @see #exclusive(Object)
 * @see #limited(int, Object)
 */
public interface ManagedResource<R> {

    /**
     * Create a new {@link ManagedResource} for which borrow actions are given
     * exclusive access to the borrowed resource. In effect, only one thread
     * will be given borrow rights for the resource at any time.
     *
     * @param resource the resource to manage
     * @return a new, exclusive {@link ManagedResource}
     */
    static <R> ManagedResource<R> exclusive(R resource) {
        return new ExclusiveManagedResource<>(resource);
    }

    /**
     * Create a new {@link ManagedResource} for which borrow actions will be given
     * shared access to the borrowed resource that is potentially being used concurrently
     * by up to <code>concurrentLimit</code> other threads.
     *
     * @param concurrentLimit the maximum number of threads that will be allowed to borrow
     *                        the resource concurrently
     * @param resource        the resource to manage
     * @return a new, concurrent-limited {@link ManagedResource}
     */
    static <R> ManagedResource<R> limited(int concurrentLimit, R resource) {
        return new LimitedManagedResource<>(concurrentLimit, resource);
    }

    /**
     * Borrow the resource and apply the given function to it.
     *
     * @param function the function to apply
     * @return a {@link Try} representing the result or exception that occurred
     * applying the function
     */
    <V> CompletableFuture<V> borrow(ThrowingFunction<R, V> function);

    /**
     * Borrow the resource with a timeout and apply the given function to it.
     *
     * @param acquireTimeout the timeout value
     * @param unit           the timeout unit
     * @param function       the function to apply
     * @return a {@link Try} representing the result or exception that occurred
     * applying the function
     */
    <V> CompletableFuture<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function);

    /**
     * Borrow the resource and perform the given action on it.
     *
     * @param action the action to execute
     * @return a {@link Try} representing the successful or exceptional result of
     * executing the action
     */
    CompletableFuture<Void> borrow(ThrowingConsumer<R> action);

    /**
     * Borrow the resource with a timeout and perform the given action on it.
     *
     * @param acquireTimeout the timeout value
     * @param unit           the timeout unit
     * @param action         the action to execute
     * @return a {@link Try} representing the successful or exceptional result of
     * executing the action
     */
    CompletableFuture<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action);

    final class ExclusiveManagedResource<R> implements ManagedResource<R> {
        private final ManagedLock lock = new ManagedLock();
        private final R resource;

        private ExclusiveManagedResource(R resource) {
            this.resource = Objects.requireNonNull(resource);
        }

        @Override
        public <V> CompletableFuture<V> borrow(ThrowingFunction<R, V> function) {
            return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, function);
        }

        @Override
        public <V> CompletableFuture<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function) {
            return lock.guard(acquireTimeout, unit, resource, function)
                    .thenApply(v -> {
                        if (v == resource) {
                            throw new UnsupportedOperationException("the resource may not be returned by the function");
                        }
                        return v;
                    });

        }

        @Override
        public CompletableFuture<Void> borrow(ThrowingConsumer<R> action) {
            return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, action);
        }

        @Override
        public CompletableFuture<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action) {
            return lock.guard(acquireTimeout, unit, resource, action);
        }
    }

    final class LimitedManagedResource<R> implements ManagedResource<R> {
        private final ManagedSemaphore semaphore;
        private final R resource;

        public LimitedManagedResource(int concurrentLimit, R resource) {
            if (concurrentLimit < 1) {
                throw new IllegalArgumentException("usage limit must be greater than zero");
            }
            this.semaphore = new ManagedSemaphore(concurrentLimit);
            this.resource = Objects.requireNonNull(resource);
        }

        @Override
        public <V> CompletableFuture<V> borrow(ThrowingFunction<R, V> function) {
            return borrow(Long.MAX_VALUE, TimeUnit.MILLISECONDS, function);
        }

        @Override
        public <V> CompletableFuture<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function) {
            return semaphore.guard(acquireTimeout, unit, resource, function)
                    .thenApply(v -> {
                        if (v == resource) {
                            throw new UnsupportedOperationException("the resource may not be returned by the function");
                        }
                        return v;
                    });
        }

        @Override
        public CompletableFuture<Void> borrow(ThrowingConsumer<R> action) {
            return semaphore.guard(resource, action);
        }

        @Override
        public CompletableFuture<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action) {
            return semaphore.guard(acquireTimeout, unit, resource, action);
        }
    }
}
