package vest.doctor.aop;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Metadata about an invoked method.
 */
public class MethodMetadata {

    private final Object containingInstance;
    private final String methodName;
    private final List<Type> methodParameters;
    private final Type returnType;

    public MethodMetadata(Object containingInstance, String methodName, List<Type> methodParameters, Type returnType) {
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
    public List<Type> getMethodParameters() {
        return methodParameters;
    }

    public Type getReturnType() {
        return returnType;
    }
}
