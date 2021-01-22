package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class MethodInvocationImpl implements MethodInvocation {

    private final MethodMetadata methodMetadata;
    private final List<MutableMethodArgument> argumentList;
    private final Callable<?> methodInvoker;
    private Object result;
    private boolean invoked = false;
    private boolean invokable = true;

    public MethodInvocationImpl(MethodMetadata methodMetadata, List<MutableMethodArgument> argumentList, Callable<?> methodInvoker) {
        this.methodMetadata = methodMetadata;
        this.argumentList = Collections.unmodifiableList(argumentList);
        this.methodInvoker = methodInvoker;
    }

    @Override
    public Object getContainingInstance() {
        return methodMetadata.getContainingInstance();
    }

    @Override
    public String getMethodName() {
        return methodMetadata.getMethodName();
    }

    @Override
    public List<TypeInfo> getMethodParameters() {
        return methodMetadata.getMethodParameters();
    }

    @Override
    public TypeInfo getReturnType() {
        return methodMetadata.getReturnType();
    }

    @Override
    public int arity() {
        return argumentList.size();
    }

    @Override
    public <T> T getArgumentValue(int i) {
        return argumentList.get(i).getValue();
    }

    @Override
    public void setArgumentValue(int i, Object o) {
        argumentList.get(i).setValue(o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke() throws Exception {
        if (!invokable) {
            throw new UnsupportedOperationException("method may not be invoked from this context");
        }
        invoked = true;
        result = methodInvoker.call();
        return (T) result;
    }

    @Override
    public boolean invoked() {
        return invoked;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    @Override
    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
        return methodMetadata.getContainingInstance().getClass().getMethod(methodMetadata.getMethodName(),
                methodMetadata.getMethodParameters().stream().map(TypeInfo::getRawType).toArray(Class<?>[]::new));
    }

    public void setInvokable(boolean invokable) {
        this.invokable = invokable;
    }
}
