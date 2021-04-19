package vest.doctor.function;

/**
 * Supplier that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;
}
