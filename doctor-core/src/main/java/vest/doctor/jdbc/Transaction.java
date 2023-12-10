package vest.doctor.jdbc;

import java.sql.Connection;

/**
 * Encapsulates the logic for a transaction.
 * Note: {@link #commit()} <strong>must</strong> be called before closing the transaction
 * or modifications will be rolled back.
 * <p>
 * Basic usage:<br/>
 * <code><pre>
 * try(Transaction tx = jdbc.transaction()){
 *     tx.connection().statement(... some update...).update();
 *     ... other modifications ...
 *     tx.commit();
 * }
 * </pre></code>
 */
public final class Transaction implements AutoCloseable {

    private boolean closed = false;
    private final JDBCConnection connection;

    Transaction(JDBC jdbc) {
        this.connection = jdbc.connection();
    }

    /**
     * Unwrap the {@link JDBCConnection} where the transaction is being executed.
     *
     * @return the connection
     */
    public JDBCConnection connection() {
        checkUsability();
        return connection;
    }

    /**
     * Commit the transaction, leaving the connection open for further actions.
     *
     * @see Connection#commit()
     */
    public void commit() {
        doWithCommonCatch(connection::commit);
    }

    /**
     * Rollback the transaction, leaving the connection open for further actions.
     *
     * @see Connection#rollback()
     */
    public void rollback() {
        doWithCommonCatch(connection::rollback);
    }

    /**
     * Determine if the transaction is closed, and thus unusable.
     *
     * @return true if the transaction has been closed
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Calls {@link java.sql.Connection#rollback()} and closes the connection
     * unless this transaction has already been closed, in which case this is a no-op.
     */
    @Override
    public void close() {
        if (!closed) {
            JDBC.closeQuietly(connection::rollback, connection);
        }
    }

    private void doWithCommonCatch(Runnable run) {
        checkUsability();
        try {
            run.run();
        } catch (Throwable t) {
            closed = true;
            JDBC.closeQuietly(connection);
            throw t;
        }
    }

    private void checkUsability() {
        if (closed) {
            throw new IllegalStateException("this transaction is closed");
        }
    }
}
