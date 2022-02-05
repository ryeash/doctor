package vest.doctor.aop;

import java.util.Iterator;

public final class AspectChainImpl implements AspectChain {
    private final Iterator<Aspect> iterator;

    AspectChainImpl(Iterable<Aspect> iterable) {
        this.iterator = iterable.iterator();
    }

    @Override
    public <T> T next(MethodInvocation methodInvocation) {
        if (iterator.hasNext()) {
            return iterator.next().execute(methodInvocation, this);
        } else {
            try {
                return methodInvocation.invoke();
            } catch (Throwable t) {
                throw new AspectException("error executing aspects", t);
            }
        }
    }
}
