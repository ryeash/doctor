package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.BiPredicate;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class or factory provider with activation predicates. The predicates will be evaluated
 * at runtime to determine if the resulting provider is activated, i.e. enabled for injection.
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Activation.Activations.class)
public @interface Activation {

    /**
     * The activation predicates to use for the marked provider source. All predicates
     * must evaluate true for the marked provider to be activated. Predicate must have a
     * public zero-arg constructor.
     *
     * @return the activation predicates
     */
    Class<? extends BiPredicate<ProviderRegistry, DoctorProvider<?>>>[] value();

    @Documented
    @Retention(RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Activations {
        Activation[] value();
    }
}
