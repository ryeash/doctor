package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.BiPredicate;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a class or factory method with activation predicates. The predicates are
 * evaluated at startup time and are ANDed together to determine if the provider should be active.
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activation {

    /**
     * The activation predicates to use for the marked provider source.
     *
     * @return the activation predicates
     */
    Class<? extends BiPredicate<ProviderRegistry, DoctorProvider<?>>>[] value();
}
