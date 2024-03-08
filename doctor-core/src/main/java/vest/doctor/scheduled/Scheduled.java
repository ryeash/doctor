package vest.doctor.scheduled;

import vest.doctor.ProviderRegistry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method to be automatically scheduled with the default {@link java.util.concurrent.ScheduledExecutorService}
 * provided by the {@link ProviderRegistry}.
 * <p>
 * Only one of {@link #interval()} or {@link #cron()} may be set.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Scheduled {

    String DEFAULT_SCHEDULED_EXECUTOR_NAME = "scheduled";

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
     * The qualifier for the {@link java.util.concurrent.ScheduledExecutorService} that will execute the
     * scheduled method;
     */
    String scheduler() default DEFAULT_SCHEDULED_EXECUTOR_NAME;

    /**
     * The interval for the schedule. See {@link Interval} for details on the format.
     */
    String interval() default "";

    /**
     * The cron schedule. See {@link Cron} for details on the format.
     */
    String cron() default "";

    /**
     * The timezone to use when evaluating the fire times for {@link #cron}.
     *
     * @see ZoneId
     */
    String timezone() default "UTC";

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
