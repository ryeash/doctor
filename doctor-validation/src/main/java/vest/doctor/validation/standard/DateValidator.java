package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiPredicate;

abstract class DateValidator<A extends Annotation, D> implements ConstraintValidator<A, D> {
    private final BiPredicate<Long, Long> predicate;

    public DateValidator(BiPredicate<Long, Long> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean isValid(D value, ConstraintValidatorContext context) {
        return value == null || predicate.test(toEpoch(value), System.currentTimeMillis());
    }

    private long toEpoch(Object o) {
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