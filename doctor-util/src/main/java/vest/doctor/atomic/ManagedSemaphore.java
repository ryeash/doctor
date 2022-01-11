package vest.doctor.atomic;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.ThrowingRunnable;
import vest.doctor.function.ThrowingSupplier;
import vest.doctor.util.Try;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper around a {@link Semaphore} that enforces proper usage.
 */
public final class ManagedSemaphore {
    private final Semaphore semaphore;

    /**
     * Create a new managed semaphore with the given number of starting permits.
     *
     * @param permits the starting permit count
     */
    public ManagedSemaphore(int permits) {
        this.semaphore = new Semaphore(permits);
    }

    /**
     * Acquire a permit, waiting indefinitely for one to become available and execute the runnable.
     *
     * @param runnable the runnable
     * @return a {@link Try} representing the success or failure of the runnable
     */
    public Try<Void> withPermit(ThrowingRunnable runnable) {
        return withPermit(Long.MAX_VALUE, TimeUnit.MILLISECONDS, runnable);
    }

    /**
     * Acquire a permit with the given timeout and
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param runnable       the runnable
     * @return a {@link Try} representing the success or failure of the runnable
     */
    public Try<Void> withPermit(long acquireTimeout, TimeUnit unit, ThrowingRunnable runnable) {
        return acquire(acquireTimeout, unit, () -> {
            runnable.runThrows();
            return null;
        });
    }

    /**
     * Acquire a permit, waiting indefinitely for one to become available
     *
     * @param supplier the supplier
     * @return a {@link Try} representing the success or failure of the supplier
     */
    public <R> Try<R> withPermit(ThrowingSupplier<R> supplier) {
        return withPermit(Long.MAX_VALUE, TimeUnit.MILLISECONDS, supplier);
    }

    /**
     * Acquire a permit with the given timeout and
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param supplier       the supplier
     * @return a {@link Try} representing the success or failure of the supplier
     */
    public <R> Try<R> withPermit(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier) {
        return acquire(acquireTimeout, unit, supplier);
    }

    /**
     * Acquire a permit, waiting indefinitely for one to become available
     *
     * @param input    the input argument
     * @param function the function
     * @return a {@link Try} representing the success or failure of applying the function
     */
    public <I, R> Try<R> withPermit(I input, ThrowingFunction<I, R> function) {
        return withPermit(Long.MAX_VALUE, TimeUnit.MILLISECONDS, input, function);
    }

    /**
     * Acquire a permit with the given timeout and
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param input          the input argument
     * @param function       the function
     * @return a {@link Try} representing the success or failure of applying the function
     */
    public <I, R> Try<R> withPermit(long acquireTimeout, TimeUnit unit, I input, ThrowingFunction<I, R> function) {
        return acquire(acquireTimeout, unit, () -> function.applyThrows(input));
    }

    /**
     * Acquire a permit, waiting indefinitely for one to become available
     *
     * @param input  the input argument
     * @param action the action
     * @return a {@link Try} representing the success or failure of the action
     */
    public <I> Try<Void> withPermit(I input, ThrowingConsumer<I> action) {
        return withPermit(Long.MAX_VALUE, TimeUnit.MILLISECONDS, input, action);
    }

    /**
     * Acquire a permit with the given timeout and
     *
     * @param acquireTimeout the timeout
     * @param unit           the unit for the timeout
     * @param input          the input argument
     * @param action         the action
     * @return a {@link Try} representing the success or failure of the action
     */
    public <I> Try<Void> withPermit(long acquireTimeout, TimeUnit unit, I input, ThrowingConsumer<I> action) {
        return withPermit(acquireTimeout, unit, () -> action.acceptThrows(input));
    }

    private <T> Try<T> acquire(long acquireTimeout, TimeUnit unit, ThrowingSupplier<T> supplier) {
        try {
            if (semaphore.tryAcquire(acquireTimeout, unit)) {
                try {
                    return Try.get(supplier);
                } finally {
                    semaphore.release();
                }
            } else {
                return Try.failure(new TimeoutException("permit acquisition timed out after " + acquireTimeout + " " + unit));
            }
        } catch (InterruptedException e) {
            return Try.failure(e);
        }
    }
}
