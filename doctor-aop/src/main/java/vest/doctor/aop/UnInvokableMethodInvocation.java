package vest.doctor.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

class UnInvokableMethodInvocation implements MethodInvocation {
    private final MethodInvocation delegate;

    public UnInvokableMethodInvocation(MethodInvocation copy) {
        this.delegate = copy;
    }

    @Override
    public Object getContainingInstance() {
        return delegate.getContainingInstance();
    }

    @Override
    public String getMethodName() {
        return delegate.getMethodName();
    }

    @Override
    public List<Type> getMethodParameters() {
        return delegate.getMethodParameters();
    }

    @Override
    public Type getReturnType() {
        return delegate.getReturnType();
    }

    @Override
    public int argumentSize() {
        return delegate.argumentSize();
    }

    @Override
    public MutableMethodArgument getArgument(int i) {
        return delegate.getArgument(i);
    }

    @Override
    public <T> T getArgumentValue(int i) {
        return delegate.getArgumentValue(i);
    }

    @Override
    public void setArgumentValue(int i, Object o) {
        delegate.setArgumentValue(i, o);
    }

    @Override
    public <T> T invoke() {
        throw new UnsupportedOperationException("method may not be invoked from this context");
    }

    @Override
    public boolean invoked() {
        return delegate.invoked();
    }

    @Override
    public <T> T getResult() {
        return delegate.getResult();
    }

    @Override
    public void setResult(Object result) {
        delegate.setResult(result);
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
        return delegate.getMethod();
    }
}
