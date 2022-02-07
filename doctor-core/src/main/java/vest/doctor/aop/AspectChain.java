package vest.doctor.aop;

/**
 * The chain of aspects to be executed during an aspected method's invocation.
 */
public sealed interface AspectChain permits AspectCoordinator.AspectChainImpl {

    /**
     * Continue the aspect execution by calling the next aspect in the chain.
     *
     * @param methodInvocation the method invocation
     * @return the result of executing the method
     */
    <T> T next(MethodInvocation methodInvocation);
}
