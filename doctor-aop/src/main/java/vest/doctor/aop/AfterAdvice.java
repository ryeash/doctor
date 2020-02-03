package vest.doctor.aop;

/**
 * Customizes a method invocation to add post-invoke workflows.
 */
public interface AfterAdvice extends Aspect {
    /**
     * Called after any {@link AroundAdvice} to customize the method after it has been invoked. In this context
     * the {@link MethodInvocation#invoke()} will throw an {@link UnsupportedOperationException}.
     *
     * @param invocation the method invocation
     */
    void after(MethodInvocation invocation);
}
