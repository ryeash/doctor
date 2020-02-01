package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method to be automatically scheduled with the default {@link java.util.concurrent.ScheduledExecutorService}
 * provided by the {@link ProviderRegistry}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Scheduled {

    enum Type {
        FIXED_DELAY, FIXED_RATE
    }

    /**
     * The period for the task, definition changes slightly based on the schedule type.
     */
    long period();

    /**
     * The unit for the {@link #period()} value.
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * The schedule type:
     * {@link Type#FIXED_RATE} corresponds to {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
     * and {@link Type#FIXED_DELAY} corresponds {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}.
     */
    Type type() default Type.FIXED_RATE;

    /**
     * Limit the number of times the scheduled method will execute per-instance of the containing object.
     * When negative, indicates that the method will be scheduled indefinitely.
     */
    int executionLimit() default -1;
}
