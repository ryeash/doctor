package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Static metadata about an invoked method.
 */
public class MethodMetadata {

    private final Object containingInstance;
    private final String methodName;
    private final List<TypeInfo> methodParameters;
    private final TypeInfo returnType;

    /**
     * Internal use only.
     *
     * @param containingInstance the object instance that the method is being called on
     * @param methodName         the nam eof the method being called
     * @param methodParameters   the parameter types for the method
     * @param returnType         the return type info for the method
     */
    public MethodMetadata(Object containingInstance, String methodName, List<TypeInfo> methodParameters, TypeInfo returnType) {
        this.containingInstance = containingInstance;
        this.methodName = methodName;
        this.methodParameters = Collections.unmodifiableList(methodParameters);
        this.returnType = returnType;
    }

    /**
     * The object instance upon which the method was invoked.
     *
     * @return the object that contains the invoked method
     */
    public Object getContainingInstance() {
        return containingInstance;
    }

    /**
     * The name of the invoked method.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * The parameters that the method accepts.
     *
     * @return a list of the parameter types
     */
    public List<TypeInfo> getMethodParameters() {
        return methodParameters;
    }

    /**
     * Information about the return type of the method.
     *
     * @return the {@link TypeInfo} for the return type, or null if the method is a void type
     */
    public TypeInfo getReturnType() {
        return returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodMetadata that = (MethodMetadata) o;
        return Objects.equals(containingInstance, that.containingInstance)
                && Objects.equals(methodName, that.methodName)
                && Objects.equals(methodParameters, that.methodParameters)
                && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containingInstance, methodName, methodParameters, returnType);
    }
}
