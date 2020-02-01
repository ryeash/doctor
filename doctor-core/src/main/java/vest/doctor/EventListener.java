package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a provided type as an event listener. The method will be wired into the {@link EventManager}
 * system and receive any published messages that match the parameter type for the method. Methods marked with this
 * annotation must have exactly one parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EventListener {
}
