package vest.doctor.util;

import vest.doctor.function.ThrowingBiConsumer;
import vest.doctor.function.ThrowingBiFunction;
import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.ThrowingPredicate;
import vest.doctor.function.ThrowingRunnable;
import vest.doctor.function.ThrowingSupplier;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Represents an execution of a task that can succeed or fail. Fundamentally it is
 * an object-oriented try-catch-finally.
 */
public record Try<T>(T result, Throwable error) {

    /**
     * Run the runnable and produce a Try to indicate success or failure.
     *
     * @param runnable the runnable
     * @return the resulting Try
     */
    public static Try<Void> run(ThrowingRunnable runnable) {
        try {
            runnable.runThrows();
            return success(null);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Run the supplier and produce a Try to indicate success (with the result)
     * or failure.
     *
     * @param supplier the supplier
     * @return the resulting Try
     */
    public static <V> Try<V> get(ThrowingSupplier<? extends V> supplier) {
        try {
            return success(supplier.getThrows());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Call the callable and produce a Try to indicate success (with the result)
     * or failure.
     *
     * @param callable the callable
     * @return the resulting Try
     */
    public static <V> Try<V> call(Callable<? extends V> callable) {
        try {
            return success(callable.call());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Run the function against the input and produce a Try to indicate success
     * (with the result from the function) or failure.
     *
     * @param input  the input to apply in the function
     * @param mapper the function
     * @return the resulting Try
     */
    public static <I, V> Try<V> apply(I input, ThrowingFunction<? super I, ? extends V> mapper) {
        try {
            return success(mapper.applyThrows(input));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Run the function against the inputs and produce a Try to indicate success (with the result from the function)
     * or failure.
     *
     * @param input1 the first input
     * @param input2 the second input
     * @param mapper the function
     * @return the resulting try
     */
    public static <I1, I2, V> Try<V> apply(I1 input1, I2 input2, ThrowingBiFunction<? super I1, ? super I2, ? extends V> mapper) {
        try {
            return success(mapper.applyThrows(input1, input2));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Check the input value using the predicate and produce a Try to indicate the success (with the boolean result)
     * or failure.
     *
     * @param input the input to test
     * @param check the predicate
     * @return the resulting Try
     */
    public static <I> Try<Boolean> test(I input, ThrowingPredicate<? super I> check) {
        try {
            return success(check.testThrows(input));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Run the action against the input and produce a Try to indicate success or failure.
     *
     * @param input  the input into the action
     * @param action the action
     * @return the resulting Try
     */
    public static <V> Try<Void> accept(V input, ThrowingConsumer<? super V> action) {
        try {
            action.acceptThrows(input);
            return success(null);
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Run the action against the inputs and produce a Try to indicate success or failure.
     *
     * @param input1 the first input
     * @param input2 the second input
     * @param action the action
     * @return the resulting Try
     */
    public static <I1, I2> Try<Void> accept(I1 input1, I2 input2, ThrowingBiConsumer<? super I1, ? super I2> action) {
        try {
            action.acceptThrows(input1, input2);
            return success(null);
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * Combine a completable future with a try.
     *
     * @param future the future to combine
     * @return a future that will never complete exceptionally, instead wrapping the result and possible
     * exception in a Try
     */
    public static <T> CompletableFuture<Try<T>> completable(CompletableFuture<T> future) {
        return future.handle(Try::new);
    }

    /**
     * Create a successful Try with the given value.
     *
     * @param value the value
     * @return a successful Try
     */
    public static <V> Try<V> success(V value) {
        return new Try<>(value, null);
    }

    /**
     * Create a failed Try with the given error.
     *
     * @param exception the exception
     * @return a failed Try
     * @throws NullPointerException if the exception is null
     */
    public static <V> Try<V> failure(Throwable exception) {
        return new Try<>(null, Objects.requireNonNull(exception));
    }

    /**
     * Get the resulting value from the Try. If the try failed, an unchecked exception
     * will be thrown.
     *
     * @return the Try result
     */
    public T get() {
        throwUnchecked();
        return result;
    }

    /**
     * If {@link #isFailure()}, get the error from the Try, else return null.
     *
     * @return the Try exception
     */
    public Throwable exception() {
        return error;
    }

    /**
     * @return true if this Try is a failure and has an exception
     */
    public boolean isFailure() {
        return error != null;
    }

    /**
     * @return true if this Try is a success
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * If this Try is a failure, throw the exception.
     *
     * @return this for chaining
     * @throws Throwable if this Try is a failure
     */
    public Try<T> throwException() throws Throwable {
        if (isFailure()) {
            throw error;
        }
        return this;
    }

    /**
     * If this Try is a failure, throw the exception (as unchecked).
     *
     * @return this for chaining
     */
    public Try<T> throwUnchecked() {
        if (isFailure()) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new RuntimeException(error);
            }
        }
        return this;
    }

    /**
     * If {@link #isSuccess()}, apply the value to the given function.
     *
     * @param fn the function
     * @return the Try result of applying the function
     */
    public <U> Try<U> apply(ThrowingFunction<? super T, ? extends U> fn) {
        if (isSuccess()) {
            return apply(result, fn);
        } else {
            return failure(error);
        }
    }

    /**
     * If {@link #isSuccess()}, perform an action on the result.
     *
     * @param action the consumer
     * @return the Try result of performing the action
     */
    public Try<T> accept(ThrowingConsumer<? super T> action) {
        try {
            if (isSuccess()) {
                action.acceptThrows(result);
            }
            return this;
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * If {@link #isSuccess()}, apply the function to the result.
     *
     * @param fn the function
     * @return the Try result of applying the function
     */
    public <U> Try<U> compose(ThrowingFunction<? super T, ? extends Try<U>> fn) {
        if (isFailure()) {
            return failure(error);
        } else {
            try {
                return fn.applyThrows(result);
            } catch (Throwable e) {
                return failure(e);
            }
        }
    }

    /**
     * If {@link #isFailure()}, apply the function to the exception.
     *
     * @param mapper the function
     * @return the Try result of applying the function to the exception
     */
    public Try<T> exceptionally(ThrowingFunction<? super Throwable, ? extends T> mapper) {
        if (isFailure()) {
            return apply(error, mapper);
        } else {
            return this;
        }
    }

    /**
     * If {@link #isFailure()}, and the exception is an instance of the given type,
     * apply the function to the exception.
     *
     * @param exceptionType the exception type
     * @param mapper        the function
     * @return the Try result of applying the function to the exception if it matches the given type, else this Try
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> exceptionally(Class<E> exceptionType, ThrowingFunction<E, ? extends T> mapper) {
        if (isFailure() && exceptionType.isInstance(error)) {
            return apply((E) error, mapper);
        } else {
            return this;
        }
    }

    /**
     * If {@link #isFailure()}, apply the function to the error, returning another Try.
     *
     * @param fn the function
     * @return the result of applying the function to the exception
     */
    public Try<T> exceptionallyCompose(ThrowingFunction<? super Throwable, ? extends Try<T>> fn) {
        if (isFailure()) {
            try {
                return fn.applyThrows(error);
            } catch (Throwable e) {
                return failure(e);
            }
        } else {
            return this;
        }
    }

    /**
     * If {@link #isFailure()}, and the exception is an instance of the given type,
     * apply the function to the exception.
     *
     * @param exceptionType the exception type
     * @param mapper        the function
     * @return the result of applying the function to the exception if it matches the given type, else this Try
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> exceptionallyCompose(Class<E> exceptionType, ThrowingFunction<E, ? extends Try<T>> mapper) {
        if (isFailure() && exceptionType.isInstance(error)) {
            try {
                return mapper.applyThrows((E) error);
            } catch (Throwable e) {
                return failure(e);
            }
        } else {
            return this;
        }
    }

    /**
     * Apply the function to the result and exception.
     *
     * @param fn the function
     * @return the Try result of applying the function
     */
    public <U> Try<U> handle(ThrowingBiFunction<? super T, Throwable, ? extends U> fn) {
        try {
            return success(fn.applyThrows(result, error));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    /**
     * If {@link #isSuccess()}, perform the action on the result. Alias for {@link #accept(ThrowingConsumer)}.
     *
     * @param consumer the action
     * @return this for chaining
     */
    public Try<T> onSuccess(ThrowingConsumer<? super T> consumer) {
        return accept(consumer);
    }

    /**
     * If {@link #isFailure()}, perform the action on the exception.
     *
     * @param consumer the action
     * @return this for chaining
     */
    public Try<T> onFailure(ThrowingConsumer<? super Throwable> consumer) {
        if (isFailure()) {
            accept(error, consumer).throwUnchecked();
        }
        return this;
    }

    /**
     * If {@link #isFailure()}, and the exception is an instance of the given type,
     * perform the action on the exception.
     *
     * @param exceptionType the exception type
     * @param consumer      the action
     * @return this for chaining
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> onFailure(Class<E> exceptionType, ThrowingConsumer<E> consumer) {
        if (isFailure() && exceptionType.isInstance(error)) {
            accept((E) error, consumer).throwUnchecked();
        }
        return this;
    }

    /**
     * Convert this Try into a {@link CompletableFuture}. The future
     * will be completed exceptionally if this Try {@link #isFailure()}.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<T> toCompletableFuture() {
        if (isFailure()) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(error);
            return failed;
        } else {
            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Turn this try into an optional value. If the try succeeded the resulting
     * optional will have the result (or be empty if the result was null). If
     * this try failed the optional will be empty.
     *
     * @return an {@link Optional} representing the result of the try
     */
    public Optional<T> toOptional() {
        return isFailure() ? Optional.empty() : Optional.ofNullable(result);
    }

    /**
     * Turn this try into a stream. If the try succeeded the resulting stream
     * will contain the result value.
     * This method will throw an exception if {@link #isFailure()}.
     *
     * @return a {@link Stream} of the result object
     */
    public Stream<T> toStream() {
        throwUnchecked();
        return Stream.of(result);
    }

    @Override
    public String toString() {
        if (isFailure()) {
            return "Try{failed: " + error + "}";
        } else {
            return "Try{success: " + result + "}";
        }
    }
}
