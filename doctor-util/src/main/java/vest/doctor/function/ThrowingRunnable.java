package vest.doctor.function;

/**
 * Runnable that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Exception;
}
