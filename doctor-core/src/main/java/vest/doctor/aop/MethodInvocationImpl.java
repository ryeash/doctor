package vest.doctor.aop;

import vest.doctor.AnnotationMetadata;
import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MethodInvocationImpl implements MethodInvocation {

    private final MethodMetadata methodMetadata;
    private final List<ArgValue> argumentList;
    private final MethodInvoker<?> methodInvoker;

    public MethodInvocationImpl(MethodMetadata methodMetadata, List<ArgValue> argumentList, MethodInvoker<?> methodInvoker) {
        this.methodMetadata = methodMetadata;
        this.argumentList = Collections.unmodifiableList(argumentList);
        this.methodInvoker = methodInvoker;
    }

    @Override
    public Object getContainingInstance() {
        return methodMetadata.containingInstance();
    }

    @Override
    public String getMethodName() {
        return methodMetadata.methodName();
    }

    @Override
    public List<TypeInfo> getMethodParameters() {
        return methodMetadata.methodParameters();
    }

    @Override
    public TypeInfo getReturnType() {
        return methodMetadata.returnType();
    }

    @Override
    public int arity() {
        return argumentList.size();
    }

    @Override
    public List<ArgValue> getArgumentValues() {
        return argumentList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArgValue getArgumentValue(int i) {
        return argumentList.get(i);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke() throws Exception {
        return (T) methodInvoker.apply(this);
    }

    @Override
    public <T> T next() {
        try {
            return invoke();
        } catch (Exception e) {
            throw new AspectException("error executing aspect method", e);
        }
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
        return methodMetadata.containingInstance()
                .getClass()
                .getMethod(methodMetadata.methodName(), methodMetadata.methodParameters().stream().map(TypeInfo::getRawType).toArray(Class<?>[]::new));
    }

    @Override
    public AnnotationMetadata annotationMetadata() {
        return methodMetadata.annotationData();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodInvocationImpl that = (MethodInvocationImpl) o;
        return Objects.equals(methodMetadata, that.methodMetadata)
                && Objects.equals(argumentList, that.argumentList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodMetadata, argumentList);
    }
}
