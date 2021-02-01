package doctor.function;

/**
 * Runnable that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Exception;

    static Runnable suppress(ThrowingRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // ignored
            }
        };
    }

    static Runnable wrap(ThrowingRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
