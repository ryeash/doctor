package vest.doctor.aop;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

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

    public Object getContainingInstance() {
        return containingInstance;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Type> getMethodParameters() {
        return methodParameters;
    }

    public Type getReturnType() {
        return returnType;
    }
}
