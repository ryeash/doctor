package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.function.BiPredicate;

abstract class NumberValidator<A extends Annotation> implements ConstraintValidator<A, Object> {
    private final BigDecimal compareTo;
    private final BiPredicate<BigDecimal, BigDecimal> predicate;

    protected NumberValidator(BigDecimal compareTo, BiPredicate<BigDecimal, BigDecimal> predicate) {
        this.compareTo = compareTo;
        this.predicate = predicate;
    }

    @Override
    public boolean isValid(Object num, ConstraintValidatorContext constraintValidatorContext) {
        // TODO: more efficiency here
        return num == null || predicate.test(new BigDecimal(num.toString()), compareTo);
    }
}