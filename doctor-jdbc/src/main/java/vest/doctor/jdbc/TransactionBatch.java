package vest.doctor.jdbc;

import vest.doctor.atomic.ManagedLock;
import vest.doctor.function.ThrowingConsumer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bundles multiple actions together to allow them to execute inside a single
 * transaction. Efforts have been taken to ensure this class is thread-safe; it is, however,
 * recommended that this class be owned and used by only one thread.
 * <p>
 * The batched actions can optionally be set to automatically execute after reaching a
 * specified limit. Use {@link #setMaxActions(int)} with value >0 to configure the
 * transaction to execute after reaching the limit. Setting max actions
 * to a negative value (the default) disables automatic execution.
 * <p>
 * Calling {@link #close()} will automatically execute all buffered actions.
 */
public final class TransactionBatch implements AutoCloseable {
    private final JDBC jdbc;
    private final ManagedLock changeLock = new ManagedLock(new ReentrantLock(true));
    private final Collection<ThrowingConsumer<JDBCConnection>> actions = new LinkedList<>();
    private int maxActions = -1;

    TransactionBatch(JDBC jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    /**
     * Set the maximum number of actions that will be buffered before automatically executing
     * the transaction.
     *
     * @param maxActions the maximum number of actions to buffer before automatically calling
     *                   {@link #execute()}; if less than zero, automatic execution is disabled
     * @return this object
     */
    public TransactionBatch setMaxActions(int maxActions) {
        this.maxActions = maxActions;
        return this;
    }

    /**
     * Add a new action to this batch. If max actions is >=0 and the limit has been reached
     * this method will automatically call {@link #execute()} before returning.
     *
     * @param action the action to add
     * @return this object
     */
    public TransactionBatch add(ThrowingConsumer<JDBCConnection> action) {
        changeLock.withLock(() -> {
            actions.add(Objects.requireNonNull(action));
        });
        if (maxActions >= 0 && actions.size() >= maxActions) {
            execute();
        }
        return this;
    }

    /**
     * Execute all actions, in the order they were added, inside a single transaction.
     * The first exception encountered will be relayed to the caller and trigger a rollback of the
     * transaction, otherwise all actions will be committed before this method returns. After this
     * method returns, the buffered actions will be cleared.
     *
     * @return this object
     */
    public TransactionBatch execute() {
        changeLock.withLock(5, TimeUnit.SECONDS, () -> {
            if (actions.isEmpty()) {
                return;
            }
            try {
                jdbc.transaction(connection -> {
                    JDBCConnection notCloseable = new UnCloseableJDBCConnection(connection);
                    for (ThrowingConsumer<JDBCConnection> action : actions) {
                        action.accept(notCloseable);
                    }
                });
            } finally {
                actions.clear();
            }
        });
        return this;
    }

    /**
     * Get the number of pending actions.
     *
     * @return the number of pending actions
     */
    public int size() {
        return actions.size();
    }

    /**
     * Clear all pending actions from this batch. This does not execute the batch.
     */
    public void clear() {
        changeLock.withLock(actions::clear);
    }

    @Override
    public void close() {
        execute();
    }

    private static final class UnCloseableJDBCConnection extends JDBCConnection {

        UnCloseableJDBCConnection(JDBCConnection c) {
            super(c.unwrap(), !c.isReusable());
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("closing the connection is not allowed inside a TransactionBatch");
        }
    }
}
