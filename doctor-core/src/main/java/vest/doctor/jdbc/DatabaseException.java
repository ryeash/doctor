package vest.doctor.jdbc;

/**
 * A general purpose unchecked exception typically used in place of the checked {@link java.sql.SQLException}
 * thrown by most jdbc operations.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable e) {
        super(message, e);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable t) {
        super(t);
    }
}
