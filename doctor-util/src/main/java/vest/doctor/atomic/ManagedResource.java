package vest.doctor.atomic;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.Try;

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
     * by up to <code>concurrentLimit</code> threads.
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
     * Borrow the resource and apply the given function to it, waiting indefinitely
     * to acquire borrow rights.
     *
     * @param function the function to apply
     * @return a {@link Try} representing the result or exception that occurred
     * applying the function
     */
    <V> Try<V> borrow(ThrowingFunction<R, V> function);

    /**
     * Borrow the resource with a timeout and apply the given function to it.
     *
     * @param acquireTimeout the timeout value
     * @param unit           the timeout unit
     * @param function       the function to apply
     * @return a {@link Try} representing the result or exception that occurred
     * applying the function
     */
    <V> Try<V> borrow(long acquireTimeout, TimeUnit unit, ThrowingFunction<R, V> function);

    /**
     * Borrow the resource and perform the given action on it, waiting indefinitely
     * to acquire borrow rights.
     *
     * @param action the action to execute
     * @return a {@link Try} representing the successful or exceptional result of
     * executing the action
     */
    Try<Void> borrow(ThrowingConsumer<R> action);

    /**
     * Borrow the resource with a timeout and perform the given action on it.
     *
     * @param acquireTimeout the timeout value
     * @param unit           the timeout unit
     * @param action         the action to execute
     * @return a {@link Try} representing the successful or exceptional result of
     * executing the action
     */
    Try<Void> borrow(long acquireTimeout, TimeUnit unit, ThrowingConsumer<R> action);

}
