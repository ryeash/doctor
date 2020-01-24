package vest.doctor.netty;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class POJOHelper<T> {
    private final T value;

    public POJOHelper(T value) {
        this.value = Objects.requireNonNull(value);
    }

    public POJOHelper<T> with(Consumer<T> action) {
        action.accept(value);
        return this;
    }

    public <V> POJOHelper<T> with(BiConsumer<T, V> setter, V param) {
        setter.accept(value, param);
        return this;
    }

    public T get() {
        return value;
    }
}
