package vest.doctor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Cron {
    // six fields
    // seconds, minutes, hours, day of month, month, day of week
    // support explicit number
    // comma delimited
    // ranges
    // keywords: @yearly, @monthly, @weekly, @hourly, @midnight

    private static final Pattern INTEGER_REGEX = Pattern.compile("\\d+");
    private static final Pattern RANGE_REGEX = Pattern.compile("([a-zA-Z0-9]+)-([a-zA-Z0-9]+)");

    public enum CronType {
        SECONDS(0, 59, null),
        MINUTES(0, 59, null),
        HOURS(0, 23, null),
        DAY_OF_MONTH(1, 31, null),
        MONTH(1, 12, Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")),
        DAY_OF_WEEK(1, 7, Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));

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
    }

    private final String expression;
    private final int[] seconds;
    private final int[] minutes;
    private final int[] hours;
    private final int[] dayOfMonth;
    private final int[] month;
    private final int[] dayOfWeek;

    public Cron(String cronExpression) {
        this.expression = cronExpression;
        String[] split = cronExpression.split("\\s+");
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
        Instant instant = Instant.ofEpochMilli(fromEpochMillis + 1000);
        ZonedDateTime next = instant.atZone(ZoneId.systemDefault());
        for (int i = 0; i < 10; i++) {
            long starting = next.toEpochSecond();

            if (seconds != null) {
                next = adjustSeconds(next, seconds);
            }
            if (minutes != null) {
                next = adjustMinutes(next, minutes);
            }
            if (hours != null) {
                next = adjustHours(next, hours);
            }
            if (dayOfWeek != null) {
                next = adjustDayOfWeek(next, dayOfWeek);
            }
            if (dayOfMonth != null) {
                next = adjustDayOfMonth(next, dayOfMonth);
            }
            if (month != null) {
                next = adjustMonths(next, month);
            }
            if (starting == next.toEpochSecond()) {
                return next.toInstant().toEpochMilli();
            }
        }
        throw new RuntimeException("failed to find the next scheduled time, this is a bug or the cron expression is too complicated: " + expression);
    }

    private static ZonedDateTime adjustMonths(ZonedDateTime date, int[] allowedValues) {
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

    private static ZonedDateTime adjustDayOfMonth(ZonedDateTime date, int[] allowedValues) {
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

    private static ZonedDateTime adjustDayOfWeek(ZonedDateTime date, int[] allowedValues) {
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

    private static ZonedDateTime adjustHours(ZonedDateTime date, int[] allowedValues) {
        int adjustment = calculateAdjustment(allowedValues, date.getHour(), 24);
        if (adjustment > 0) {
            return date.plusHours(adjustment)
                    .withSecond(0)
                    .withMinute(0);
        } else {
            return date;
        }
    }

    private static ZonedDateTime adjustMinutes(ZonedDateTime date, int[] allowedValues) {
        int adjustment = calculateAdjustment(allowedValues, date.getMinute(), 60);
        if (adjustment > 0) {
            return date.plusMinutes(adjustment)
                    .withSecond(0);
        } else {
            return date;
        }
    }

    private static ZonedDateTime adjustSeconds(ZonedDateTime date, int[] allowedValues) {
        int adjustment = calculateAdjustment(allowedValues, date.getSecond(), 60);
        if (adjustment > 0) {
            return date.plusSeconds(adjustment);
        } else {
            return date;
        }
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
}
