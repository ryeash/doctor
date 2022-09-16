package vest.doctor.aop;

import vest.doctor.TypeInfo;

/**
 * A mutable wrapper around a single method invocation argument value.
 */
public sealed interface ArgValue permits ArgValueImpl {

    /**
     * Get the type information for the parameter.
     */
    TypeInfo type();

    /**
     * The name of the parameter as it appears in the source code.
     */
    String name();

    /**
     * Get the parameter value.
     */
    <A> A get();

    /**
     * Set the parameter value.
     */
    void set(Object value);
}
