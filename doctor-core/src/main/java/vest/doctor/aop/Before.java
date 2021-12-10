package vest.doctor.aop;

/**
 * Customizes a method to add pre-invoke workflows.
 */
public non-sealed interface Before extends Aspect {

    /**
     * Called before any {@link Around} to customize the method before it has been invoked. In this context
     * the {@link MethodInvocation#invoke()} will throw an {@link UnsupportedOperationException}.
     *
     * @param invocation the method invocation
     */
    void before(MethodInvocation invocation);
}
