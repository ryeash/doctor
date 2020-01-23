package vest.doctor.aop;

public class MutableMethodArgument {
    private Object value;

    public MutableMethodArgument(Object value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
