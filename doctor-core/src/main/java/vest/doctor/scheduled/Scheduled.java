package vest.doctor.scheduled;

import vest.doctor.ProviderRegistry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method to be automatically scheduled with the default {@link java.util.concurrent.ScheduledExecutorService}
 * provided by the {@link ProviderRegistry}.
 * <p>
 * One and only one of {@link #interval()} or {@link #cron()} may be set.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Scheduled {

    enum Type {
        /**
         * corresponds to {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
         */
        FIXED_DELAY,
        /**
         * corresponds to {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
         */
        FIXED_RATE
    }

    /**
     * The interval for the schedule. See {@link Interval} for details on the format.
     */
    String interval() default "";

    /**
     * The cron schedule. See {@link Cron} for details on the format.
     */
    String cron() default "";

    /**
     * The schedule type. Only applies when using {@link #interval()}.
     */
    Type type() default Type.FIXED_RATE;

    /**
     * Limit the number of times the scheduled method will execute per-instance of the containing object.
     * A negative value indicates that the method will be scheduled indefinitely.
     */
    long executionLimit() default -1;
}
