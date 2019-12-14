package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Scheduled {

    enum Type {
        FIXED_DELAY, FIXED_RATE
    }

    long period();

    TimeUnit unit() default TimeUnit.MILLISECONDS;

    Type type() default Type.FIXED_RATE;
}
