package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.util.Collections;
import java.util.List;

/**
 * Static metadata about an invoked method.
 */
public class MethodMetadata {

    private final Object containingInstance;
    private final String methodName;
    private final List<TypeInfo> methodParameters;
    private final TypeInfo returnType;

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

    public TypeInfo getReturnType() {
        return returnType;
    }
}
