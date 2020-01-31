package vest.doctor.aop;

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

    public MethodInvocationImpl(MethodMetadata methodMetadata, List<MutableMethodArgument> argumentList, Callable<?> methodInvoker) {
        this.methodMetadata = methodMetadata;
        this.argumentList = Collections.unmodifiableList(argumentList);
        this.methodInvoker = methodInvoker;
    }

    @Override
    public MethodMetadata getMetadata() {
        return methodMetadata;
    }

    @Override
    public int argumentSize() {
        return argumentList.size();
    }

    @Override
    public MutableMethodArgument getArgument(int i) {
        return argumentList.get(i);
    }

    @Override
    public <T> T getArgumentValue(int i) {
        return getArgument(i).getValue();
    }

    @Override
    public void setArgumentValue(int i, Object o) {
        getArgument(i).setValue(o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke() throws Exception {
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
                methodMetadata.getMethodParameters().stream().map(t -> (Class<?>) t).toArray(Class<?>[]::new));
    }
}
