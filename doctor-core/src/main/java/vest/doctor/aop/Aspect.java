package vest.doctor.aop;

/**
 * Marker class for all aspect interfaces.
 */
public interface Aspect {

    /**
     * Execute the method invocation, altering behavior as required.
     *
     * @param methodInvocation the method invocation
     * @param chain            the aspect chain to use when continuing aspect processing
     * @return the result of invoking the method
     */
    <T> T execute(MethodInvocation methodInvocation, AspectChain chain);
}
