package vest.doctor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Type information for a parameter, field, or return value.
 */
public final class TypeInfo {
    private final Class<?> rawType;
    private final List<TypeInfo> parameterTypes;

    /**
     * Create a new type information object with the given raw type and parameter types.
     *
     * @param rawType        the raw type
     * @param parameterTypes the parameter types
     */
    public TypeInfo(Class<?> rawType, TypeInfo... parameterTypes) {
        this(rawType, List.of(parameterTypes));
    }

    /**
     * Create a new type information object with the given raw type and parameter types.
     *
     * @param rawType        the raw type
     * @param parameterTypes the parameter types
     */
    public TypeInfo(Class<?> rawType, List<TypeInfo> parameterTypes) {
        this.rawType = rawType;
        if (rawType != null) {
            this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        } else {
            this.parameterTypes = Collections.emptyList();
        }
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

    /**
     * Determine if this TypeInfo is compatible with the given type and parameter types.
     *
     * @param type           the raw type to test
     * @param parameterTypes the parameter type to test
     * @return true if this TypeInfo instance is type compatible with the given type/parameters
     */
    public boolean matches(Class<?> type, Class<?>... parameterTypes) {
        if (type.isAssignableFrom(rawType)) {
            if (this.parameterTypes.size() == parameterTypes.length) {
                for (int i = 0; i < this.parameterTypes.size(); i++) {
                    Class<?> t = parameterTypes[i];
                    Class<?> compare = this.parameterTypes.get(i).rawType;
                    if (!t.isAssignableFrom(compare)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(rawType.getName());
        if (hasParameterizedTypes()) {
            sb.append(parameterTypes.stream().map(String::valueOf).collect(Collectors.joining(", ", "<", ">")));
        }
        return sb.toString();
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
