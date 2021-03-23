package vest.doctor;

/**
 * General purpose unchecked exception used in generated classes.
 */
public class InjectionException extends RuntimeException {

    public InjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
