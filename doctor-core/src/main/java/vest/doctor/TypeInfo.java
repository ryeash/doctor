package vest.doctor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Information about the type of a parameter or field.
 */
public class TypeInfo {
    private final Class<?> rawType;
    private final List<TypeInfo> parameterTypes;

    public TypeInfo(Class<?> rawType, TypeInfo... parameterTypes) {
        this(rawType, parameterTypes != null ? Arrays.asList(parameterTypes) : Collections.emptyList());
    }

    public TypeInfo(Class<?> rawType, List<TypeInfo> parameterTypes) {
        this.rawType = Objects.requireNonNull(rawType);
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
    }

    /**
     * The raw type of the parameter.
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * A list of any parameterized types declared on the target parameter.
     */
    public List<TypeInfo> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Return true if the list returned from {@link #getParameterTypes()} will be non-empty.
     */
    public boolean hasParameterizedTypes() {
        return parameterTypes != null && !parameterTypes.isEmpty();
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "rawType=" + rawType +
                ", parameterTypes=" + parameterTypes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeInfo typeInfo = (TypeInfo) o;
        return Objects.equals(rawType, typeInfo.rawType) &&
                Objects.equals(parameterTypes, typeInfo.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawType, parameterTypes);
    }
}
