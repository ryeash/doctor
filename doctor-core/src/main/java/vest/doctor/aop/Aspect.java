package vest.doctor.aop;

/**
 * Marker class for all aspect interfaces.
 * <br><br>
 * In general an aspect can be broken into three steps - before, execution, and after:
 * <code><pre>
 * {@literal @}Singleton
 * public class DemoAspect implements Aspect {
 *  public <T> T execute(MethodInvocation methodInvocation) {
 *      // before
 *      System.out.println("this is happening before execution");
 *
 *      // execution
 *      // either allow the aspect chain to proceed
 *      Object result = methodInvocation.next();
 *      // or, short circuit
 *      String result = "my result";
 *
 *      // after
 *      System.out.println("the result of the method: " + result);
 *      return (T) result;
 *  }
 * }
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
