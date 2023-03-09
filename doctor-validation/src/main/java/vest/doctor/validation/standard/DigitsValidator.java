package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class DigitsValidator implements ConstraintValidator<Digits, Object> {
    private final int integer;
    private final int fraction;

    public DigitsValidator(int integer, int fraction) {
        this.integer = integer;
        this.fraction = fraction;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BigDecimal n = new BigDecimal(value.toString());
        int beforeDecimal = n.signum() == 0 ? 1 : n.precision() - n.scale();
        return beforeDecimal <= integer && n.scale() <= fraction;
    }
}