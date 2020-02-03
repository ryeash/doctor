package vest.doctor.aop;

/**
 * A mutable wrapper around a method argument.
 */
public class MutableMethodArgument {
    private Object value;

    public MutableMethodArgument(Object value) {
        this.value = value;
    }

    /**
     * Get the argument value.
     *
     * @return the argument value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    /**
     * Set the argument value.
     *
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
