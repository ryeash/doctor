package vest.doctor.scheduled;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Cron string parser and evaluator. Used internally to support {@link Scheduled} methods using cron expressions.
 * <p>
 * This implementation of cron requires 6 fields:
 * <code>seconds, minutes, hours, day-of-month, month, day-of-week</code>
 * <pre>
 * Field definitions:
 * - second: the second of the minute, restricted to [0, 59]
 * - minute: the minute of the hour, restricted to [0, 59]
 * - hour: the hour of the day (24hr), restricted to [0, 23]
 * - day of month: the day of the month, restricted to [1, 28 29 30 or 31], varies with the month (and leap year)
 * - month: the month of the year, restricted to [1, 12], January is 1, December is 12
 * - day of week: the day of the week, restricted to [1, 7], the week runs Monday (1) to Sunday (7).
 * Example:
 * 0 5 * * * *
 * | | | | | - every day of the week
 * | | | | - every month
 * | | | - every day
 * | | - every hour
 * | - 5 minutes past the hour
 * - 0 seconds past the minute
 * The fire times will be, e.g
 * Jan 1, 2020 00:05:00
 * Jan 1, 2020 01:05:00
 * Jan 1, 2020 02:05:00
 * Jan 1, 2020 03:05:00
 *
 * Supported field definitions:
 * - a constant number, e.g. `5` will enforce fire times having only that value for the field
 * - a comma delimited list of numbers, e.g. `5,10,15` will enforce fire times having one of those values for the field
 * - ranges, e.g `5-25` will enforce fire times having any number in the range (inclusive) for the field
 * - wildcard, e.g. `*` any value for the field is allowed in a fire time
 *
 * Aliases for some field are supported:
 * - Month: the first 3 letters of the english month name, all caps, are supported for the month expression, e.g. `JAN`, `NOV`, etc.
 * - Day of week: the first 3 letter of the english day name, all caps, are supported for the day of week, e.g. `MON`, `SAT`
 *
 * There are built in macros with pre-defined cron expressions:
 * <code>@yearly</code>: "0 0 0 1 JAN *" - every year at midnight on January 1st
 * <code>@monthly</code>: "0 0 0 1 * *" - every month at midnight of the 1st day
 * <code>@weekly</code>: "0 0 0 * * SUN" - every week at midnight on Sunday
 * <code>@midnight</code>: "0 0 0 * * *" - every day at midnight
 * <code>@hourly</code>: "0 0 * * * *" - every hour at the top of the hour
 * </pre>
 */
public final class Cron {

    private static final Pattern INTEGER_REGEX = Pattern.compile("\\d+");
    private static final Pattern RANGE_REGEX = Pattern.compile("([a-zA-Z\\d]+)-([a-zA-Z\\d]+)");

    public enum CronType {
        SECONDS(0, 59, null) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getSecond(), 60);
                if (adjustment > 0) {
                    return date.plusSeconds(adjustment);
                } else {
                    return date;
                }
            }
        },

        MINUTES(0, 59, null) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getMinute(), 60);
                if (adjustment > 0) {
                    return date.plusMinutes(adjustment)
                            .withSecond(0);
                } else {
                    return date;
                }
            }
        },

        HOURS(0, 23, null) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getHour(), 24);
                if (adjustment > 0) {
                    return date.plusHours(adjustment)
                            .withSecond(0)
                            .withMinute(0);
                } else {
                    return date;
                }
            }
        },

        DAY_OF_MONTH(1, 31, null) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getDayOfMonth(), date.getMonth().length(isLeapYear(date)));
                if (adjustment > 0) {
                    return date.plusDays(adjustment)
                            .withSecond(0)
                            .withMinute(0)
                            .withHour(0);
                } else {
                    return date;
                }
            }
        },

        MONTH(1, 12, List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getMonth().getValue(), 12);
                if (adjustment > 0) {
                    return date.plusMonths(adjustment)
                            .withSecond(0)
                            .withMinute(0)
                            .withHour(0)
                            .withDayOfMonth(1);
                } else {
                    return date;
                }
            }
        },

        DAY_OF_WEEK(1, 7, List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
            @Override
            public ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues) {
                int adjustment = calculateAdjustment(allowedValues, date.getDayOfWeek().getValue(), 7);
                if (adjustment > 0) {
                    return date.plusDays(adjustment)
                            .withSecond(0)
                            .withMinute(0)
                            .withHour(0);
                } else {
                    return date;
                }
            }
        };

        private final int rangeStart;
        private final int rangeEnd;
        private final List<String> aliases;

        CronType(int rangeStart, int rangeEnd, List<String> aliases) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.aliases = aliases;
            if (aliases != null && aliases.size() != count()) {
                throw new IllegalArgumentException("the number of aliases must match the range of the integer values");
            }
        }

        public boolean isValid(int num) {
            return rangeStart <= num && num <= rangeEnd;
        }

        public int convert(String alias) {
            if (INTEGER_REGEX.matcher(alias).matches()) {
                int i = Integer.parseInt(alias);
                if (isValid(i)) {
                    return i;
                }
            } else if (aliases != null) {
                int index = aliases.indexOf(alias);
                if (index >= 0) {
                    return index + rangeStart;
                }
            }
            throw new IllegalArgumentException("invalid value for " + this + " field of cron string: " + alias);
        }

        public int count() {
            return rangeEnd - rangeStart + 1;
        }

        public int[] parse(String str) {
            // wildcard, effectively any number
            if (str.equals("*")) {
                return null;
            }
            // range notation: 1-10, 10-12, etc.
            Matcher matcher = RANGE_REGEX.matcher(str);
            if (matcher.matches()) {
                String startStr = matcher.group(1).trim();
                String endStr = matcher.group(2).trim();
                return IntStream.rangeClosed(convert(startStr), convert(endStr)).sorted().toArray();
            }
            // assume comma separated list of numbers, also covers single number
            else {
                return Stream.of(str.split(",")).map(String::trim).mapToInt(this::convert).sorted().toArray();
            }
        }

        public abstract ZonedDateTime adjust(ZonedDateTime date, int[] allowedValues);
    }

    private final String expression;
    private final int[] seconds;
    private final int[] minutes;
    private final int[] hours;
    private final int[] dayOfMonth;
    private final int[] month;
    private final int[] dayOfWeek;

    public Cron(String cronExpression) {
        this.expression = translateMacro(cronExpression);
        String[] split = expression.split("\\s+");
        if (split.length != 6) {
            throw new IllegalArgumentException("invalid cron expression [" + cronExpression + "]; expression must have 6 segments, white space delimited");
        }

        seconds = CronType.SECONDS.parse(split[0].trim());
        minutes = CronType.MINUTES.parse(split[1].trim());
        hours = CronType.HOURS.parse(split[2].trim());
        dayOfMonth = CronType.DAY_OF_MONTH.parse(split[3].trim());
        month = CronType.MONTH.parse(split[4].trim());
        dayOfWeek = CronType.DAY_OF_WEEK.parse(split[5].trim());
    }

    public long nextFireTime() {
        return nextFireTime(System.currentTimeMillis());
    }

    public long nextFireTime(long fromEpochMillis) {
        ZonedDateTime next = Instant.ofEpochMilli(fromEpochMillis + 1000)
                .atZone(ZoneId.systemDefault());
        for (int i = 0; i < 15; i++) {
            long starting = next.toEpochSecond();

            if (seconds != null) {
                next = CronType.SECONDS.adjust(next, seconds);
            }
            if (minutes != null) {
                next = CronType.MINUTES.adjust(next, minutes);
            }
            if (hours != null) {
                next = CronType.HOURS.adjust(next, hours);
            }
            if (dayOfWeek != null) {
                next = CronType.DAY_OF_WEEK.adjust(next, dayOfWeek);
            }
            if (dayOfMonth != null) {
                next = CronType.DAY_OF_MONTH.adjust(next, dayOfMonth);
            }
            if (month != null) {
                next = CronType.MONTH.adjust(next, month);
            }
            if (starting == next.toEpochSecond()) {
                return next.toInstant().toEpochMilli();
            }
        }
        throw new RuntimeException("failed to find the next scheduled time, this is a bug or the cron expression is too specific/complicated: " + expression);
    }

    @Override
    public String toString() {
        return expression;
    }

    private static int calculateAdjustment(int[] allowedValues, int currentValue, int maxValue) {
        int value = closestValue(currentValue, allowedValues);
        int adjustment = 0;
        if (currentValue < value) {
            adjustment = value - currentValue;
        } else if (currentValue > value) {
            adjustment = (maxValue - currentValue) + value;
        }
        return adjustment;
    }

    private static int closestValue(int current, int[] allowedValues) {
        if (allowedValues.length == 1) {
            return allowedValues[0];
        }
        for (int value : allowedValues) {
            if (value >= current) {
                return value;
            }
        }
        return allowedValues[0];
    }

    private static boolean isLeapYear(ZonedDateTime date) {
        int year = date.getYear();
        return (year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0));
    }

    private static String translateMacro(String expression) {
        return switch (expression.trim()) {
            case "@yearly" -> "0 0 0 1 JAN *";
            case "@monthly" -> "0 0 0 1 * *";
            case "@weekly" -> "0 0 0 * * SUN";
            case "@midnight" -> "0 0 0 * * *";
            case "@hourly" -> "0 0 * * * *";
            default -> expression;
        };
    }
}
