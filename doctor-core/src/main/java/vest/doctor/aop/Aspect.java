package vest.doctor.aop;

/**
 * Marker class for all aspect interfaces.
 */
public interface Aspect {

    /**
     * Execute the method invocation, altering behavior as required.
     *
     * @param methodInvocation the method invocation
     * @return the result of invoking the method
     */
    <T> T execute(MethodInvocation methodInvocation);
}
