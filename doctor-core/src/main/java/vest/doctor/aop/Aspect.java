package vest.doctor.aop;

/**
 * An aspect modifies invocations of methods to add or modify behavior; generally for crosscutting
 * functionality that should be applied to many methods, e.g. logging and tracing.
 *
 * <br><br>
 * In general an aspect can be broken into three steps - before, execution, and after:
 * <code><pre>
 * {@literal @}Singleton
 *  public class DemoAspect implements Aspect {
 *   public Object execute(MethodInvocation methodInvocation) {
 *       // before
 *       System.out.println("this is happening before execution");
 *
 *       // execution
 *       // either allow the aspect chain to proceed
 *       Object result = methodInvocation.next();
 *       // or, short circuit by invoking the method directly
 *       Object result = methodInvocation.invoke();
 *       // or, short circuit by returning a cached result
 *       Object result = "my result";
 *
 *       // after
 *       System.out.println("the result of the method: " + result);
 *       return result;
 *   }
 *  }
 *
 * </pre></code>
 */
public interface Aspect {

    /**
     * Execute the method invocation, altering behavior as required.
     *
     * @param methodInvocation the method invocation
     * @return the result of invoking the method
     */
    Object execute(MethodInvocation methodInvocation);
}
