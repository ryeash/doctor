package vest.doctor.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Time and date related utilities.
 */
public class Clock {

    /**
     * Number of milliseconds in one second
     */
    public static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1);

    /**
     * Number of milliseconds in one minute
     */
    public static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

    /**
     * Number of milliseconds in one hour
     */
    public static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);

    /**
     * Number of milliseconds in one standard day
     */
    public static final long ONE_DAY = TimeUnit.DAYS.toMillis(1);

    /**
     * Number of milliseconds in seven standard days
     */
    public static final long ONE_WEEK = ONE_DAY * 7;

    /**
     * Number of milliseconds in thirty standard days
     */
    public static final long ONE_MONTH = ONE_DAY * 30;

    /**
     * Number of milliseconds in 365 standard days
     */
    public static final long ONE_YEAR = ONE_DAY * 365;

    /**
     * Alias for {@link System#currentTimeMillis()}
     */
    public static long epochMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Alias for {@link System#currentTimeMillis()} / 1000
     */
    public static long epochSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Alias for {@link System#currentTimeMillis()}
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * The current epoch time, converted to the number of given time units.
     */
    public static long now(TimeUnit unit) {
        return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * The current epoch millisecond time plus an offset.
     */
    public static long timePlus(long offset, TimeUnit unit) {
        return System.currentTimeMillis() + unit.toMillis(offset);
    }

    /**
     * Truncate the given epoch millisecond time to the nearest given unit.
     * <p>
     * Example:
     * <code>
     * new Date(truncate(Clock.now(), TimeUnit.HOURS));
     * </code>
     * would return the current time with the minutes, seconds, and milliseconds set to zero
     *
     * @param epochMilliseconds the time to truncate
     * @param unit              the units to truncate
     * @return the truncated time
     */
    public static long truncate(long epochMilliseconds, TimeUnit unit) {
        return truncate(epochMilliseconds, unit.toMillis(1));
    }

    /**
     * Truncate the given epoch millisecond time to closest increment that is less than the given time.
     * <p>
     * Example:
     * <code>
     * new Date(truncate(Clock.now(), 1000));
     * </code>
     * would return the current time with the milliseconds zeroed out
     *
     * @param epochMilliseconds the time to truncate
     * @param truncation        the truncation increment
     * @return the truncated time
     */
    public static long truncate(long epochMilliseconds, long truncation) {
        return truncation * (epochMilliseconds / truncation);
    }

    /**
     * Round the given epoch millisecond time to the nearest whole time unit.
     *
     * @param epochMilliseconds the time to round
     * @param unit              the unit to round to
     * @return the rounded time
     */
    public static long round(long epochMilliseconds, TimeUnit unit) {
        return round(epochMilliseconds, unit.toMillis(1));
    }

    /**
     * Round the given epoch millisecond time to the nearest whole time increment.
     *
     * @param epochMilliseconds the time to round
     * @param increment         the increment to round to
     * @return the rounded time
     */
    public static long round(long epochMilliseconds, long increment) {
        return Math.round((double) epochMilliseconds / (double) (increment)) * (increment);
    }

    /**
     * Get an HTTP compliant date string (suitable for e.g. the 'Date' header) for the current time.
     */
    public static String httpTime() {
        return httpTime(System.currentTimeMillis());
    }

    /**
     * Get an HTTP compliant date string (suitable for e.g. the 'Date' header) for the given time.
     */
    public static String httpTime(long epochMillis) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * Pause the current thread for the given number of milliseconds, quietly ignoring the potential
     * {@link InterruptedException}.
     */
    public static void sleepQuietly(long millis) {
        sleepQuietly(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Pause the current thread for the given duration, quietly ignoring the potential
     * {@link InterruptedException}.
     *
     * @param duration the duration to wait
     * @param unit     the unit for the duration
     */
    public static void sleepQuietly(long duration, TimeUnit unit) {
        try {
            unit.sleep(duration);
        } catch (InterruptedException e) {
            // ignored
        }
    }

    /**
     * Time the execution of the given runnable and return the duration.
     *
     * @param runnable the runnable to execute
     * @return the duration of the execution
     */
    public static Duration stopwatch(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return Duration.ofNanos(System.nanoTime() - start);
    }
}
