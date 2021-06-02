package vest.doctor.function;

/**
 * Runnable that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingRunnable extends Runnable {

    default void run() {
        try {
            runThrows();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void runThrows() throws Exception;
}
