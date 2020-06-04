package vest.doctor.aop;

/**
 * Customizes a method invocation to add around-invoke workflows.
 */
public interface Around extends Aspect {

    /**
     * Called to invoke the method. Implementations should, but don't have to,
     * call {@link MethodInvocation#invoke()}.
     *
     * @param invocation the method invocation
     */
    void execute(MethodInvocation invocation);
}
