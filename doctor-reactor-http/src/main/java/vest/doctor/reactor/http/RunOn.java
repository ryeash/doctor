package vest.doctor.reactor.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set the {@link reactor.core.scheduler.Scheduler} to use to run the endpoint method (or methods if marked at the class level).
 * The scheduler must be provided via the {@link vest.doctor.ProviderRegistry}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RunOn {

    /**
     * The name of the default executor that will always be provided by the container.
     */
    String DEFAULT_SCHEDULER = "workerGroup";

    /**
     * The qualifier of the provided {@link java.util.concurrent.ExecutorService} that will execute
     * the endpoint method (or methods when marked at the class level).
     */
    String value() default DEFAULT_SCHEDULER;
}
