package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.AssertFalse;

public final class AssertFalseValidator implements ConstraintValidator<AssertFalse, Boolean> {
    @Override
    public boolean isValid(Boolean value, ConstraintValidatorContext context) {
        return value == null || !value;
    }
}