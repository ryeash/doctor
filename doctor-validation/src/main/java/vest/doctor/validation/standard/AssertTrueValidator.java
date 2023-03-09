package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.AssertTrue;

public final class AssertTrueValidator implements ConstraintValidator<AssertTrue, Boolean> {
    @Override
    public boolean isValid(Boolean aBoolean, ConstraintValidatorContext constraintValidatorContext) {
        return aBoolean == null || aBoolean;
    }
}