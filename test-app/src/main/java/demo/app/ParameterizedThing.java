package demo.app;

public class ParameterizedThing<T> {

    private final T value;

    public ParameterizedThing(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
