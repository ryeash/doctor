package vest.doctor.validation.standard;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class DoctorConstraintValidatorImpl<T> implements DoctorConstraintValidator<Object> {

    public static DoctorConstraintValidator<Object> assertFalse(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(AssertFalse.class, true,
                DoctorConstraintValidatorImpl::convertBoolean,
                b -> !b,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> assertTrue(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(AssertTrue.class, true,
                DoctorConstraintValidatorImpl::convertBoolean,
                b -> b,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> decimalMax(BigDecimal max, boolean inclusive, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(DecimalMax.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> {
                    int c = val.compareTo(max);
                    return inclusive ? c <= 0 : c < 0;
                },
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> decimalMin(BigDecimal min, boolean inclusive, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(DecimalMin.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> {
                    int c = val.compareTo(min);
                    return inclusive ? c >= 0 : c > 0;
                },
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> digits(int integer, int fraction, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Digits.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> {
                    int beforeDecimal = val.signum() == 0 ? 1 : val.precision() - val.scale();
                    return beforeDecimal <= integer && val.scale() <= fraction;
                },
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> email(String invalidMessage) {
        return pattern("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$", invalidMessage);
    }

    public static DoctorConstraintValidator<Object> pattern(String regex, String invalidMessage) {
        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(regex);
        return new DoctorConstraintValidatorImpl<>(Pattern.class, true,
                String::valueOf,
                val -> regexPattern.matcher(val).matches(),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> future(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Future.class, true,
                DoctorConstraintValidatorImpl::toEpochMillis,
                val -> val > System.currentTimeMillis(),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> futureOrPresent(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(FutureOrPresent.class, true,
                DoctorConstraintValidatorImpl::toEpochMillis,
                val -> val >= System.currentTimeMillis(),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> past(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Past.class, true,
                DoctorConstraintValidatorImpl::toEpochMillis,
                val -> val < System.currentTimeMillis(),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> pastOrPresent(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(PastOrPresent.class, true,
                DoctorConstraintValidatorImpl::toEpochMillis,
                val -> val <= System.currentTimeMillis(),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> max(long max, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Max.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.longValue() < max,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> min(long min, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Min.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.longValue() > min,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> negative(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Negative.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.compareTo(BigDecimal.ZERO) < 0,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> negativeOrZero(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(NegativeOrZero.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.compareTo(BigDecimal.ZERO) <= 0,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> positive(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Positive.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.compareTo(BigDecimal.ZERO) > 0,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> positiveOrZero(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(PositiveOrZero.class, true,
                DoctorConstraintValidatorImpl::convertNumber,
                val -> val.compareTo(BigDecimal.ZERO) >= 0,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> notBlank(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(NotBlank.class, false,
                String::valueOf,
                val -> val.chars().anyMatch(i -> !Character.isWhitespace(i)),
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> notEmpty(String invalidMessage) {
        return size(1, Integer.MAX_VALUE, invalidMessage);
    }

    public static DoctorConstraintValidator<Object> size(int min, int max, String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Size.class, true,
                Function.identity(),
                val -> {
                    int size;
                    if (val instanceof CharSequence cs) {
                        size = cs.length();
                    } else if (val instanceof Collection<?> c) {
                        size = c.size();
                    } else if (val instanceof Map<?, ?> m) {
                        size = m.size();
                    } else if (val.getClass().isArray()) {
                        size = ((Object[]) val).length;
                    } else {
                        throw new UnsupportedOperationException("unable to validate @Size for " + val);
                    }
                    return min <= size && size <= max;
                },
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> notNull(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(NotNull.class, false,
                Function.identity(),
                Objects::nonNull,
                invalidMessage);
    }

    public static DoctorConstraintValidator<Object> isNull(String invalidMessage) {
        return new DoctorConstraintValidatorImpl<>(Null.class, false,
                Function.identity(),
                Objects::isNull,
                invalidMessage);
    }

    private final Class<? extends Annotation> constraint;
    private final boolean nullIsValid;
    private final Function<Object, T> typeConverter;
    private final Predicate<T> validator;
    private final String invalidMessage;

    protected DoctorConstraintValidatorImpl(Class<? extends Annotation> constraint, boolean nullIsValid,
                                            Function<Object, T> typeConverter,
                                            Predicate<T> validator,
                                            String invalidMessage) {
        this.constraint = constraint;
        this.nullIsValid = nullIsValid;
        this.typeConverter = typeConverter;
        this.validator = validator;
        this.invalidMessage = invalidMessage;
    }

    @Override
    public ValidationException validate(Object value) {
        if (value == null) {
            return nullIsValid() ? null : new ValidationException("null value not valid");
        }
        return Optional.of(value)
                .map(this::convert)
                .map(this::isValid)
                .map(this::checkSuccess)
                .orElse(null);
    }

    public Class<? extends Annotation> constraint() {
        return constraint;
    }

    protected boolean nullIsValid() {
        return nullIsValid;
    }

    protected T convert(Object value) {
        return typeConverter.apply(value);
    }

    protected boolean isValid(T value) {
        return validator.test(value);
    }

    protected String message() {
        return invalidMessage;
    }

    protected ValidationException checkSuccess(boolean success) {
        return success ? null : new ValidationException(message());
    }

    private static BigDecimal convertNumber(Object o) {
        if (o instanceof BigDecimal bd) {
            return bd;
        } else if (o instanceof BigInteger bi) {
            return new BigDecimal(bi);
        } else if (o instanceof Number n) {
            return new BigDecimal(n.toString());
        } else if (o instanceof CharSequence s) {
            return new BigDecimal(s.toString());
        } else {
            throw new ValidationException("invalid value, expected a number, got " + o);
        }
    }

    private static Boolean convertBoolean(Object o) {
        if (o instanceof Boolean b) {
            return b;
        } else {
            throw new UnsupportedOperationException("expected a boolean value");
        }
    }

    private static long toEpochMillis(Object o) {
        if (o instanceof Date d) {
            return d.getTime();
        } else if (o instanceof Calendar c) {
            return c.getTime().getTime();
        } else if (o instanceof Instant i) {
            return i.toEpochMilli();
        } else if (o instanceof TemporalAccessor ta) {
            return Instant.from(ta).toEpochMilli();
        } else {
            throw new UnsupportedOperationException("can not validate date object: " + o);
        }
    }
}
