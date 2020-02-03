package vest.doctor.aop;

/**
 * General purpose runtime exception to indicate a failure during aspect execution.
 */
public class AspectException extends RuntimeException {

    public AspectException(String message, Throwable cause) {
        super(message, cause);
    }
}
