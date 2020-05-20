package vest.doctor.netty;

import java.util.Objects;
import java.util.function.BiConsumer;

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
