package vest.doctor.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on a class or method to indicate which aspects to use when invoking the method(s). When a class is marked
 * with this annotation, <strong>all</strong> methods in the class will be wired with the listed aspects.
 * <p>
 * Aspects are executed in the order that they appear in this annotation. When aspects are marked at the class
 * level as well as the method level, the aspects on the class are executed first (in order) then those
 * on the method.
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
