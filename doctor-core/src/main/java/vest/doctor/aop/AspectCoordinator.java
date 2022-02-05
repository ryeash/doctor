package vest.doctor.aop;

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
        return new AspectChainImpl(aspects).next(methodInvocation);
    }
}
