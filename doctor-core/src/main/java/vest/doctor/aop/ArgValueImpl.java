package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.util.Objects;

public final class ArgValueImpl implements ArgValue {
    private final TypeInfo type;
    private final String name;
    private Object value;

    public ArgValueImpl(TypeInfo type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    @Override
    public TypeInfo type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A get() {
        return (A) value;
    }

    @Override
    public void set(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArgValueImpl argValue = (ArgValueImpl) o;
        return Objects.equals(type, argValue.type)
                && Objects.equals(name, argValue.name)
                && Objects.equals(value, argValue.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, value);
    }
}
