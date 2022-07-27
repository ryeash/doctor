package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A jdbc object interceptor that intercepts various objects created by {@link JDBC} and {@link JDBCConnection}
 * so that the classes can be configured, wrapped, etc.
 */
public interface JDBCInterceptor {

    /**
     * Intercept a newly acquired {@link Connection} from the {@link javax.sql.DataSource}.
     *
     * @param connection the connection to configure
     * @return the connection to use for jdbc operations
     * @throws SQLException for any error while configuring the connection
     */
    default Connection intercept(Connection connection) throws SQLException {
        // no-op
        return connection;
    }

    /**
     * Intercept a newly created {@link Statement} from a {@link Connection}.
     *
     * @param statement the statement to configure
     * @return the statement to use for jdbc operations
     * @throws SQLException for any error while configuring the connection
     */
    default <T extends Statement> T intercept(T statement) throws SQLException {
        // no-op
        return statement;
    }

    /**
     * An interceptor that sets the auto-commit flag on connections.
     *
     * @param autoCommit the auto-commit state to set
     */
    record AutoCommit(boolean autoCommit) implements JDBCInterceptor {
        @Override
        public Connection intercept(Connection connection) throws SQLException {
            connection.setAutoCommit(autoCommit);
            return connection;
        }
    }

    /**
     * An interceptor that sets the read-only flag on connections.
     *
     * @param readOnly the read-only state to set
     */
    record ReadOnly(boolean readOnly) implements JDBCInterceptor {
        @Override
        public Connection intercept(Connection connection) throws SQLException {
            connection.setReadOnly(readOnly);
            return connection;
        }
    }
}
