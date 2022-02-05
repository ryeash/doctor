package vest.doctor.aop;

import vest.doctor.AnnotationMetadata;
import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MethodInvocationImpl implements MethodInvocation {

    private final MethodMetadata methodMetadata;
    private final List<Object> argumentList;
    private final MethodInvoker<?> methodInvoker;

    public MethodInvocationImpl(MethodMetadata methodMetadata, List<Object> argumentList, MethodInvoker<?> methodInvoker) {
        this.methodMetadata = methodMetadata;
        this.argumentList = new ArrayList<>(argumentList);
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
    @SuppressWarnings("unchecked")
    public <T> T getArgumentValue(int i) {
        return (T) argumentList.get(i);
    }

    @Override
    public void setArgumentValue(int i, Object o) {
        argumentList.set(i, o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke() throws Exception {
        return (T) methodInvoker.apply(this);
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
