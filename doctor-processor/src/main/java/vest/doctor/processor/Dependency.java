package vest.doctor.processor;

import vest.doctor.processing.ProviderDependency;

import javax.lang.model.element.TypeElement;
import java.util.Objects;

class Dependency implements ProviderDependency {
    private final TypeElement type;
    private final String qualifier;
    private final boolean required;

    public Dependency(TypeElement type, String qualifier) {
        this(type, qualifier, true);
    }

    public Dependency(TypeElement type, String qualifier, boolean required) {
        this.type = Objects.requireNonNull(type);
        this.qualifier = qualifier;
        this.required = required;
    }

    @Override
    public TypeElement type() {
        return type;
    }

    @Override
    public String qualifier() {
        return qualifier;
    }

    @Override
    public boolean required() {
        return required;
    }

    @Override
    public String toString() {
        return "{" +
               "type=" + type +
               ", qualifier=" + qualifier +
               ", required=" + required +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderDependency)) {
            return false;
        }
        Dependency that = (Dependency) o;
        return Objects.equals(type.asType().toString(), that.type.asType().toString())
               && Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type.asType().toString(), qualifier);
    }
}
