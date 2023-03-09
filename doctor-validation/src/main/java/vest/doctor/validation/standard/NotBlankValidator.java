package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotBlank;

public class NotBlankValidator implements ConstraintValidator<NotBlank, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value != null && value.chars().anyMatch(i -> !Character.isWhitespace(i));
    }
}