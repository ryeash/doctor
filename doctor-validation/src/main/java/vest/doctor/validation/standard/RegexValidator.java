package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

public abstract class RegexValidator<A extends Annotation> implements ConstraintValidator<A, CharSequence> {
    private final Pattern regex;

    public RegexValidator(Pattern regex) {
        this.regex = regex;
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return regex.matcher(value).matches();
    }
}