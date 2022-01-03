package vest.doctor.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A name/value pair to decorate a method. Attributes are available in aspects via
 * {@link MethodInvocation#attributes()}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(Attributes.class)
public @interface Attribute {

    /**
     * The name of the attribute. Can be interpolated from properties using '${property.name}' notation.
     */
    String name();

    /**
     * The value of the attribute. Can be interpolated from properties using '${property.name}' notation.
     */
    String value();
}
