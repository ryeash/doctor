package vest.doctor.function;

import java.util.function.Supplier;

/**
 * Supplier that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> extends Supplier<T> {

    default T get() {
        try {
            return getThrows();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    T getThrows() throws Exception;
}
