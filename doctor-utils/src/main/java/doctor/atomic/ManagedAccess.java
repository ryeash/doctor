package doctor.atomic;

import doctor.function.ThrowingConsumer;
import doctor.function.ThrowingFunction;
import doctor.function.ThrowingRunnable;
import doctor.function.ThrowingSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Generalized contract for abstracting access control using different concurrent tools,
 * e.g. {@link java.util.concurrent.locks.Lock}, {@link java.util.concurrent.Semaphore}.
 */
public interface ManagedAccess {

    /**
     * Switch to asynchronous mode. Managed actions will be performed in a separate thread (via {@link ForkJoinPool#commonPool()}).
     *
     * @return this lock
     */
    ManagedAccess async();

    /**
     * Switch to asynchronous mode. Managed actions will be performed in a separate thread (via the given executor).
     *
     * @param executorService the executor that will be used to execute the actions
     * @return this lock
     */
    ManagedAccess async(ExecutorService executorService);

    /**
     * Swithc to synchronous mode. Managed actions will be performed in the calling thread.
     *
     * @return this lock
     */
    ManagedAccess sync();

    /**
     * Perform the given action after acquiring access, waiting indefinitely.
     *
     * @param runnable the action
     * @return the success or failure result of the action
     */
    CompletableFuture<Void> guard(ThrowingRunnable runnable);

    /**
     * Perform the given action after acquiring access, waiting the given timeout before failing.
     *
     * @param acquireTimeout the lock acquire timeout
     * @param unit           the unit for the timeout
     * @param runnable       the action
     * @return the success or failure result of the action
     */
    CompletableFuture<Void> guard(long acquireTimeout, TimeUnit unit, ThrowingRunnable runnable);

    /**
     * Perform the given function after acquiring access, with the given input, waiting indefinitely.
     *
     * @param input    the input to the function
     * @param function the function to apply the input
     * @return the result of executing the function
     */
    <I, R> CompletableFuture<R> guard(I input, ThrowingFunction<I, R> function);

    /**
     * Perform the given function after acquiring access, with the given input, waiting the given timeout before failing.
     *
     * @param acquireTimeout the lock acquire timeout
     * @param unit           the unit for the timeout
     * @param input          the input to the function
     * @param function       the function to apply the input
     * @return the result of executing the function
     */
    <I, R> CompletableFuture<R> guard(long acquireTimeout, TimeUnit unit, I input, ThrowingFunction<I, R> function);

    /**
     * Perform the given action after acquiring access, waiting indefinitely.
     *
     * @param supplier the action
     * @return the result of executing the action
     */
    <R> CompletableFuture<R> guard(ThrowingSupplier<R> supplier);

    /**
     * Perform the given action after acquiring access, waiting the given timeout before failing.
     *
     * @param acquireTimeout the lock acquire timeout
     * @param unit           the unit for the timeout
     * @param supplier       the action
     * @return the result of executing the action
     */
    <R> CompletableFuture<R> guard(long acquireTimeout, TimeUnit unit, ThrowingSupplier<R> supplier);

    /**
     * Perform the given action after acquiring access, waiting indefinitely.
     *
     * @param input  the input to the action
     * @param action the action
     * @return the success or failure result of the action
     */
    <I> CompletableFuture<Void> guard(I input, ThrowingConsumer<I> action);

    /**
     * Perform the given action after acquiring access, waiting the given timeout before failing.
     *
     * @param acquireTimeout the lock acquire timeout
     * @param unit           the unit for the timeout
     * @param input          this input to the action
     * @param action         the action
     * @return the success or failure result of the action
     */
    <I> CompletableFuture<Void> guard(long acquireTimeout, TimeUnit unit, I input, ThrowingConsumer<I> action);
}
