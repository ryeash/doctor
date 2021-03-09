package vest.doctor.scheduled;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a time interval, with magnitude and unit.
 * <p>
 * The format for an interval string is 'magnitude[unit alias]'
 * The magnitude is an integer
 * <p>
 * Supported units are: Days, Hours, Minutes, Seconds, Milliseconds (default), Microseconds, and Nanoseconds<br/>
 * Shorthand strings for the units are supported:<br/>
 * Days: d, day, days<br/>
 * Hours: h, hour, hours<br/>
 * Minutes: m, min, minute, minutes<br/>
 * Seconds: s, sec, second, seconds<br/>
 * Milliseconds: ms, millis, millisecond, milliseconds<br/>
 * Microseconds: u, us, microsecond, microseconds<br/>
 * Nanoseconds: ns, nanosecond, nanoseconds<br/>
 * <p>
 * Examples:
 * 10s -> 10 seconds
 * 50ms -> 50 milliseconds
 * 12345 us -> 12345 microseconds
 */
public class Interval {

    private static final Pattern INTERVAL_REGEX = Pattern.compile("^([0-9]+)\\s*([a-zA-Z]+)?");

    public static boolean matches(String intervalString) {
        return INTERVAL_REGEX.matcher(intervalString).matches();
    }

    private final long magnitude;
    private final TimeUnit unit;

    /**
     * Create a new interval from an interval string.
     *
     * @param intervalString the string to parse as an interval
     */
    public Interval(String intervalString) {
        Objects.requireNonNull(intervalString);
        Matcher matcher = INTERVAL_REGEX.matcher(intervalString.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("malformed interval string, must be in the form <number>[<unit>]");
        }
        this.magnitude = Long.parseLong(matcher.group(1).trim());
        this.unit = readUnit(matcher.group(2));
    }

    /**
     * Get the magnitude of the interval.
     */
    public long getMagnitude() {
        return magnitude;
    }

    /**
     * Get the time unit for the interval.
     */
    public TimeUnit getUnit() {
        return unit;
    }

    private static TimeUnit readUnit(String unit) {
        if (unit == null) {
            return TimeUnit.MILLISECONDS;
        }
        return switch (unit.trim().toLowerCase()) {
            case "d", "day", "days" -> TimeUnit.DAYS;
            case "h", "hour", "hours" -> TimeUnit.HOURS;
            case "m", "min", "minute", "minutes" -> TimeUnit.MINUTES;
            case "s", "sec", "second", "seconds" -> TimeUnit.SECONDS;
            case "ms", "millis", "millisecond", "milliseconds", "" -> TimeUnit.MILLISECONDS;
            case "u", "us", "microsecond", "microseconds" -> TimeUnit.MICROSECONDS;
            case "ns", "nanosecond", "nanoseconds" -> TimeUnit.NANOSECONDS;
            default -> throw new IllegalArgumentException("unknown duration unit: " + unit);
        };
    }
}
