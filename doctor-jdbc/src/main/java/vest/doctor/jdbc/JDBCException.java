package vest.doctor.jdbc;

/**
 * General purpose exception class used in the JDBC module. Most methods in <code>java.sql</code> code throw checked
 * exceptions, this class is used primarily so downstream users aren't forced to deal with checked exceptions everywhere.
 */
public class JDBCException extends RuntimeException {

    /**
     * @see RuntimeException#RuntimeException(String)
     */
    public JDBCException(String msg) {
        super(msg);
    }

    /**
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public JDBCException(String msg, Throwable t) {
        super(msg, t);
    }
}
