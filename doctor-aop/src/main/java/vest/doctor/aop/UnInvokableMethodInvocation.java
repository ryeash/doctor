package vest.doctor.aop;

import java.lang.reflect.Method;

public class UnInvokableMethodInvocation implements MethodInvocation {
    private final MethodInvocation delegate;

    public UnInvokableMethodInvocation(MethodInvocation copy) {
        this.delegate = copy;
    }

    @Override
    public MethodMetadata getMetadata() {
        return delegate.getMetadata();
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
