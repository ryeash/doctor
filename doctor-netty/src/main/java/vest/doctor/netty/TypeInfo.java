package vest.doctor.netty;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TypeInfo {
    private Class<?> rawType;
    private List<TypeInfo> parameterTypes;

    public TypeInfo(Class<?> rawType, TypeInfo... parameterTypes) {
        this(rawType, parameterTypes != null ? Arrays.asList(parameterTypes) : Collections.emptyList());
    }

    public TypeInfo(Class<?> rawType, List<TypeInfo> parameterTypes) {
        this.rawType = rawType;
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getRawType() {
        return rawType;
    }

    public List<TypeInfo> getParameterTypes() {
        return parameterTypes;
    }

    public boolean hasParameterizedTypes() {
        return parameterTypes != null && !parameterTypes.isEmpty();
    }

    public JavaType jacksonType(ObjectMapper mapper) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return mapper.getTypeFactory().constructType(rawType);
        } else {
            JavaType[] javaTypes = parameterTypes.stream().map(t -> t.jacksonType(mapper)).toArray(JavaType[]::new);
            return mapper.getTypeFactory().constructParametricType(rawType, javaTypes);
        }
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "rawType=" + rawType +
                ", parameterTypes=" + parameterTypes +
                '}';
    }
}
