package doctor.stream;

import java.util.Objects;

final class DistinctKey<T> {
    private final T value;
    private final Object key;

    DistinctKey(T value, Object key) {
        this.value = value;
        this.key = key;
    }

    T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || (o != null && getClass() == o.getClass() && Objects.equals(key, ((DistinctKey<?>) o).key));
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
