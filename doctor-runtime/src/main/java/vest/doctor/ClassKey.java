package vest.doctor;

import java.util.Objects;

class ClassKey implements Comparable<ClassKey> {

    private final String type;

    ClassKey(Class<?> type) {
        this.type = type.getCanonicalName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassKey that = (ClassKey) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public int compareTo(ClassKey o) {
        return this.type.compareTo(o.type);
    }
}
