package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Represents an invocation of a method. Providing details about the method that was called as well as the arguments the
 * method was called with. Allows manipulation of arguments and invocation result.
 */
public interface MethodInvocation {

    /**
     * Get the instance upon which the method was invoked.
     *
     * @return the containing instance
     */
    Object getContainingInstance();

    /**
     * Get the name of the method that was invoked.
     *
     * @return the method name
     */
    String getMethodName();

    /**
     * Get the parameter types for the method that was invoked.
     *
     * @return the parameter types
     */
    List<TypeInfo> getMethodParameters();

    /**
     * Get the type of the returned value.
     *
     * @return the return type
     */
    TypeInfo getReturnType();

    /**
     * Get the number of arguments the method was invoked with.
     *
     * @return the number of arguments
     */
    int arity();

    /**
     * Get the value of an argument from a specific position.
     *
     * @param i the position of the argument to get
     * @return the argument
     */
    <T> T getArgumentValue(int i);

    /**
     * Set the value of an argument at a specific position.
     *
     * @param i the position of the argument to set
     * @param o the argument
     */
    void setArgumentValue(int i, Object o);

    /**
     * Invoke the method.
     *
     * @return the result of invocation
     * @throws Exception for any error encountered
     */
    <T> T invoke() throws Exception;

    /**
     * Check if the underlying method has been invoked.
     *
     * @return true if {@link #invoke()} has been called one or more times
     */
    boolean invoked();

    /**
     * Get the result of the method invocation.
     *
     * @return the method invocation result
     */
    <T> T getResult();

    /**
     * Set (or override) the result of the method invocation.
     *
     * @param result the result to set
     */
    void setResult(Object result);

    /**
     * Get the {@link Method} that was invoked.
     * <p>
     * Note: One of the main reasons to use a code generation library like Doctor is to avoid
     * reflection; use of this method may indicate a problem in design.
     *
     * @return the invoked method
     * @throws NoSuchMethodException if the generated code is not able to determine the invoked method
     */
    Method getMethod() throws NoSuchMethodException;

    /**
     * Get the attributes attached to the method via {@link Attributes}.
     *
     * @return the aspect attributes
     */
    Map<String, String> attributes();
}
