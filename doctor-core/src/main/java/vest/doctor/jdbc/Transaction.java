package vest.doctor.jdbc;

/**
 *
 */
public final class Transaction implements AutoCloseable {

    private boolean closed = false;
    private final JDBCConnection connection;

    Transaction(JDBC jdbc) {
        this.connection = jdbc.connection();
    }

    public JDBCConnection connection() {
        checkUsability();
        return connection;
    }

    public void commit() {
        doWithCommonCatch(connection::commit);
    }

    public void rollback() {
        doWithCommonCatch(connection::rollback);
    }

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
