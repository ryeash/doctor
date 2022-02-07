package vest.doctor.aop;

import java.util.Iterator;
import java.util.List;

/**
 * Internally used to coordinate aspect execution.
 */
public final class AspectCoordinator {
    private final List<Aspect> aspects;

    public AspectCoordinator(Aspect... delegates) {
        aspects = List.of(delegates);
    }

    public <T> T call(MethodInvocation methodInvocation) {
        return new AspectChainImpl(aspects.iterator()).next(methodInvocation);
    }

    record AspectChainImpl(Iterator<Aspect> iterator) implements AspectChain {
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
}
