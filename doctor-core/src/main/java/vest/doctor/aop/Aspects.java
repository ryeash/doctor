package vest.doctor.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on a class or method to indicate which aspects to use when invoking the method(s). When a class is marked
 * with this annotation, <strong>all</strong> methods in the class will be wired with the listed aspects.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Aspects {
    /**
     * The aspects to apply to the method or class methods; in the order they will be executed.
     */
    Class<? extends Aspect>[] value();
}
