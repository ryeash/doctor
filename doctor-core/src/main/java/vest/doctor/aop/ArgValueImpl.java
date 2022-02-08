package vest.doctor.aop;

import vest.doctor.TypeInfo;

public final class ArgValueImpl<T> implements ArgValue<T> {
    private final TypeInfo type;
    private final String name;
    private T value;

    public ArgValueImpl(TypeInfo type, String name, T value) {
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
    public void set(T value) {
        this.value = value;
    }
}
