package vest.doctor.http.server.rest;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Used internally to assist the code generator when building {@link BeanParam} parameters.
 */
public final class POJOHelper<T> {
    private final T value;

    public POJOHelper(T value) {
        this.value = Objects.requireNonNull(value);
    }

    public <V> POJOHelper<T> with(BiConsumer<T, V> setter, V param) {
        setter.accept(value, param);
        return this;
    }

    public T get() {
        return value;
    }
}
