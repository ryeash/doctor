package vest.doctor.scheduled;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a time interval, with magnitude and unit.
 * <p>
 * The format for an interval string is 'magnitude[unit alias]'
 * The magnitude is an integer
 * <p>
 * Supported units are: Days, Hours, Minutes, Seconds, Milliseconds (default), Microseconds, and Nanoseconds
 * Shorthand strings for the units are supported:
 * Days: d, day, days
 * Hours: h, hour, hours
 * Minutes: m, min, minute, minutes
 * Seconds: s, sec, second, seconds
 * Microseconds: u, us, microsecond, microseconds
 * Nanoseconds: ns, nanosecond, nanoseconds
 * <p>
 * Examples:
 * 10s -> 10 seconds
 * 50ms -> 50 milliseconds
 * 12345 us -> 12345 microseconds
 */
public class Interval {

    private static final Pattern INTERVAL_REGEX = Pattern.compile("([0-9]+)\\s*([a-zA-Z]+)?");

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
        Matcher matcher = INTERVAL_REGEX.matcher(intervalString);
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
        switch (unit.trim().toLowerCase()) {
            case "d":
            case "day":
            case "days":
                return TimeUnit.DAYS;
            case "h":
            case "hour":
            case "hours":
                return TimeUnit.HOURS;
            case "m":
            case "min":
            case "minute":
            case "minutes":
                return TimeUnit.MINUTES;
            case "s":
            case "sec":
            case "second":
            case "seconds":
                return TimeUnit.SECONDS;
            case "ms":
            case "millis":
            case "millisecond":
            case "milliseconds":
            case "":
                return TimeUnit.MILLISECONDS;
            case "u":
            case "us":
            case "microsecond":
            case "microseconds":
                return TimeUnit.MICROSECONDS;
            case "ns":
            case "nanosecond":
            case "nanoseconds":
                return TimeUnit.NANOSECONDS;
            default:
                throw new IllegalArgumentException("unknown duration unit: " + unit);
        }
    }
}
