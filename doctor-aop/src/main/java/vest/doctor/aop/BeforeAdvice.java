package vest.doctor.aop;

/**
 * Customized a method to add pre-invoke workflows.
 */
public interface BeforeAdvice extends Aspect {

    /**
     * Called before any {@link AroundAdvice} to customize the method before it has been invoked. In this context
     * the {@link MethodInvocation#invoke()} will throw an {@link UnsupportedOperationException}.
     *
     * @param invocation the method invocation
     */
    void before(MethodInvocation invocation);
}
