package doctor.function;

import java.util.function.Supplier;

/**
 * Supplier that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;

    static <T> Supplier<T> suppress(ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                return null;
            }
        };
    }

    static <T> Supplier<T> wrap(ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
