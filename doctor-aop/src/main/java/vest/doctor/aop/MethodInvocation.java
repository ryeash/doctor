package vest.doctor.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

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
     * Get the parameter type for the method that was invoked.
     *
     * @return the parameter types
     */
    List<Type> getMethodParameters();

    /**
     * Get the type of the returned value.
     *
     * @return the return type
     */
    Type getReturnType();

    /**
     * Get the number of arguments the method was invoked with.
     *
     * @return the number of arguments
     */
    int argumentSize();

    /**
     * Get an argument from a specific position.
     *
     * @param i the position of the argument to get
     * @return the argument
     */
    MutableMethodArgument getArgument(int i);

    /**
     * Get the value of an argument from a specific position.
     *
     * @param i the position of the argument to get
     * @return the argument
     */
    <T> T getArgumentValue(int i);

    /**
     * Set the value of an argument at a specific postiion.
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
     * @return true if {@link #invoke()} has been called
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
     *
     * @return the invoked method
     * @throws NoSuchMethodException if the generated code is not able to determine the invoked method
     */
    Method getMethod() throws NoSuchMethodException;
}
