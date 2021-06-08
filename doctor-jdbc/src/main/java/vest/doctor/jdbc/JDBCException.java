package vest.doctor.jdbc;

/**
 * General purpose exception class used in the JDBC module. Most methods in <code>java.sql</code> code throw checked
 * exceptions, this class is used primarily so downstream users aren't forced to deal with checked exceptions everywhere.
 */
public class JDBCException extends RuntimeException {

    public JDBCException(String msg) {
        super(msg);
    }

    public JDBCException(String msg, Throwable t) {
        super(msg, t);
    }

    public static RuntimeException unchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        } else {
            throw new JDBCException("error executing jdbc actions", t);
        }
    }
}
