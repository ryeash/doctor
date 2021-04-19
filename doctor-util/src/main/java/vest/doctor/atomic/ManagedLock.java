package vest.doctor.atomic;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.ThrowingRunnable;
import vest.doctor.function.ThrowingSupplier;
import vest.doctor.function.Try;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrapper around a {@link Lock} that enforces proper lock usage.
 */
public final class ManagedLock {
    private final Lock lock;

    /**
     * Create a new managed lock using a {@link ReentrantLock} as the underlying lock implementation.
     */
    public ManagedLock() {
        this(new ReentrantLock());
    }

    /**
     * Create a new managed lock using the given lock implementation.
     *
     * @param lock the underlying lock to use
     */
    public ManagedLock(Lock lock) {
        this.lock = Objects.requireNonNull(lock);
    }

    /**
     * Acquire the lock, waiting indefinitely if necessary, and execute the runnable.
     *
     * @param runnable the runnable
     * @return a {@link Try} representing the success or failure of the runnable
     */
    public Try<Void> withLock(ThrowingRunnable runnable) {
        return withLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS, runnable);
    }

    /**
     * Acquire the lock with the given timeout and execute the the runnable.
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the acquire timeout
     * @param runnable       the runnable
     * @return a {@link Try} representing the success or failure of the runnable
     */
    public Try<Void> withLock(long acquireTimeout, TimeUnit unit, ThrowingRunnable runnable) {
        return guard(acquireTimeout, unit, () -> Try.run(runnable));
    }

    /**
     * Acquire the lock, waiting indefinitely if necessary, and apply the function to the input.
     *
     * @param input    the input argument
     * @param function the function
     * @return a {@link Try} representing the success or failure of applying the function
     */
    public <I, R> Try<R> withLock(I input, ThrowingFunction<I, R> function) {
        return withLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS, input, function);
    }

    /**
     * Acquire the lock with the given timeout and apply the function to the input.
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param input          the input argument
     * @param function       the function
     * @return a {@link Try} representing the success or failure of applying the function
     */
    public <I, R> Try<R> withLock(long acquireTimeout, TimeUnit unit, I input, ThrowingFunction<I, R> function) {
        return guard(acquireTimeout, unit, () -> Try.apply(input, function));
    }

    /**
     * Acquire the lock, waiting indefinitely if necessary, and execute the supplier.
     *
     * @param supplier the supplier
     * @return a {@link Try} representing the success or failure of calling the supplier
     */
    public <R> Try<R> withLock(ThrowingSupplier<R> supplier) {
        return withLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS, supplier);
    }

    /**
     * Acquire the lock with the given timeout and execute the supplier.
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param supplier       the supplier
     * @return a {@link Try} representing the success or failure of executing the supplier
     */
    public <R> Try<R> withLock(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier) {
        return guard(acquireTimeout, unit, () -> Try.supply(supplier));
    }

    /**
     * Acquire the lock, waiting indefinitely if necessary, and execute the action with the input.
     *
     * @param input  the input argument
     * @param action the action
     * @return a {@link Try} representing the success or failure of executing the action
     */
    public <I> Try<Void> withLock(I input, ThrowingConsumer<I> action) {
        return withLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS, () -> action.accept(input));
    }

    /**
     * Acquire the lock with the given timeout and execute the actino with the input.
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param input          the input argument
     * @param action         the action
     * @return a {@link Try} representing the success or failure of executing the action
     */
    public <I> Try<Void> withLock(long acquireTimeout, TimeUnit unit, I input, ThrowingConsumer<I> action) {
        return withLock(acquireTimeout, unit, () -> action.accept(input));
    }

    private <R> Try<R> guard(long acquireTimeout, TimeUnit unit, ThrowingSupplier<Try<R>> supplier) {
        try {
            if (lock.tryLock(acquireTimeout, unit)) {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    return Try.failure(e);
                } finally {
                    lock.unlock();
                }
            } else {
                return Try.failure(new TimeoutException("lock acquisition timed out after " + acquireTimeout + " " + unit));
            }
        } catch (InterruptedException e) {
            return Try.failure(e);
        }
    }
}
