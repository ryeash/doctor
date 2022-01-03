package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a class or factory method with activation modules.
 * <p>
 * For classes annotated with modules all {@link Factory} method providers
 * will automatically be marked with the modules listed on the containing class.
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Modules {

    /**
     * The modules for which the provider(s) will be active.
     */
    String[] value();
}
