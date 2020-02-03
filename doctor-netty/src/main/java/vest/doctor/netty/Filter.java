package vest.doctor.netty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the method should be added to the filter sequence for eligible requests.
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Filter {

    /**
     * The {@link FilterStage} for the filter.
     */
    FilterStage value() default FilterStage.BEFORE_ROUTE;
}