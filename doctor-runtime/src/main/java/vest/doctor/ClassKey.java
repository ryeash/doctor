package vest.doctor;

import java.util.Objects;

class ClassKey implements Comparable<ClassKey> {

    private final String type;
    private final int typeHash;

    ClassKey(Class<?> type) {
        this.type = type.getCanonicalName();
        this.typeHash = type.hashCode();
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
        return typeHash == that.typeHash &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return typeHash;
    }

    @Override
    public int compareTo(ClassKey o) {
        return this.type.compareTo(o.type);
    }
}
