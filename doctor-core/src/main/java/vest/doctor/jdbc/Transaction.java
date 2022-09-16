package vest.doctor.jdbc;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A database transaction that uses a lazily allocated connection to execute buffered/deferred actions.
 * Actions are buffered with calls to {@link #accept(Consumer)} and {@link #apply(Function)}. When {@link #commit()}
 * is called a connection is allocated and the actions are executed using it. The actions are executed in the order
 * they were buffered using {@link JDBC#inTransaction(Consumer)}, so all commit/rollback guarantees of that method
 * hold true when this transaction commits.
 * <p><br/>
 * This transaction is only valid for one commit (or rollback), i.e. once committed, this transaction is
 * no longer usable and attempting to buffer additional actions will throw {@link IllegalStateException}.
 * <p><br/>
 * {@link CompletableFuture Completable futures} are used to relay success/failure to the caller when
 * buffering actions. The futures will complete successfully if the transaction commits successfully, should
 * any action fail (and thus invalidate the transaction) the exception thrown by the underlying commit
 * operation will be relayed to all buffered action futures and the connection will be rolled back.
 * <p><br/>
 * The {@link AutoCloseable#close()} method for this class will call {@link #commit()}, so usage can be as simple
 * as:
 * <pre>
 * // try-with-resource block will automatically commit the transaction
 * try (Transaction tx = jdbc.transaction()) {
 *     ... buffer actions ...
 * }
 * </pre>
 */
public final class Transaction implements AutoCloseable {

    private final JDBC jdbc;
    private final List<DeferredAction<?>> actions = new LinkedList<>();
    private boolean closed = false;

    Transaction(JDBC jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Buffer a consumer action to take on the transaction connection.
     *
     * @param action the action to buffer
     * @return the {@link CompletableFuture} that will relay the result of executing the action
     */
    public CompletableFuture<Void> accept(Consumer<JDBCConnection> action) {
        return apply(new JDBCUtils.ConsumerFunction<>(action));
    }

    /**
     * Buffer a function to apply to the transaction connection.
     *
     * @param function the action to buffer
     * @param <R>      the result type from the mapping function
     * @return the {@link CompletableFuture} that will relay the result of executing the function
     */
    public <R> CompletableFuture<R> apply(Function<? super JDBCConnection, ? extends R> function) {
        checkUsability();
        CompletableFuture<R> future = new CompletableFuture<>();
        synchronized (actions) {
            actions.add(new DeferredAction<>(function, future));
        }
        return future;
    }

    /**
     * Commit this transaction. All buffered actions will be executed (in order) using a single
     * {@link JDBCConnection} and the results will be relayed to the {@link CompletableFuture action futures}.
     * The first error encountered will be relayed to the caller as well as the action futures.
     * After this method completes (normally or exceptionally) this transaction will be considered 'closed'
     * and no longer usable.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void commit() {
        checkUsability();
        closed = true;
        try {
            jdbc.inTransaction(jdbcConnection -> {
                for (DeferredAction action : actions) {
                    try {
                        action.result = JDBCUtils.allowedFunctionReturn(action.function.apply(jdbcConnection));
                    } catch (Throwable t) {
                        action.future.completeExceptionally(t);
                        throw t;
                    }
                }
            });
        } catch (Throwable t) {
            for (DeferredAction deferredAction : actions) {
                if (!deferredAction.future.isDone()) {
                    deferredAction.future.completeExceptionally(t);
                }
            }
            throw t;
        } finally {
            for (DeferredAction deferredAction : actions) {
                if (!deferredAction.future.isDone()) {
                    deferredAction.future.complete(deferredAction.result);
                }
            }
            actions.clear();
        }
    }

    /**
     * Rollback this transaction. Due to the lazy nature of this object, no rollback command is issued (since
     * no connection has been allocated until the {@link #commit()} method is called), so, functionally, this
     * method just clears the buffered actions and marks this transaction as closed.
     */
    public void rollback() {
        checkUsability();
        closed = true;
        actions.clear();
    }

    /**
     * Get the closed state of the transaction.
     *
     * @return true if this transaction has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Calls the {@link #commit()} method unless this transaction has already been closed,
     * in which case this is a no-op.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        commit();
    }

    private void checkUsability() {
        if (closed) {
            throw new IllegalStateException("this transaction is closed");
        }
    }

    private static final class DeferredAction<R> {
        final Function<? super JDBCConnection, ? extends R> function;
        final CompletableFuture<R> future;
        Object result;

        public DeferredAction(Function<? super JDBCConnection, ? extends R> function, CompletableFuture<R> future) {
            this.function = function;
            this.future = future;
        }
    }
}
