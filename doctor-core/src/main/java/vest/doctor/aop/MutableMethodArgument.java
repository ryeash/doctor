package vest.doctor.aop;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MutableMethodArgument that = (MutableMethodArgument) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
