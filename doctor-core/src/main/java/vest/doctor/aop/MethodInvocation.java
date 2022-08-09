package vest.doctor.aop;

import vest.doctor.AnnotationMetadata;
import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Represents an invocation of a method; providing details about the method that was called as well as the arguments the
 * method was called with.
 */
public sealed interface MethodInvocation permits MethodInvocationImpl, AspectCoordinator.ChainedInvocation {

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
     * Get the list of arguments this invocation was called with.
     */
    List<ArgValue> getArgumentValues();

    /**
     * Get the value of an argument from a specific position.
     *
     * @param i the position of the argument to get
     * @return the argument
     */
    ArgValue getArgumentValue(int i);

    /**
     * Invoke the method.
     * <p>
     * Note: unless the aspect needs to explicitly execute the method (which can short
     * circuit aspect execution), {@link #next()} should be used instead.
     *
     * @return the result of invocation
     * @throws Exception for any error encountered
     */
    <T> T invoke() throws Exception;

    /**
     * Continue this method invocation by passing it to the next configured aspect, or,
     * if there are no more aspects to apply, invoke the method and return the result.
     *
     * @return the result of invocation
     */
    <T> T next();

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
     * Get the {@link AnnotationMetadata} for the method being called.
     */
    AnnotationMetadata annotationMetadata();
}
