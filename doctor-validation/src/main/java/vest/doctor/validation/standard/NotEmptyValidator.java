package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.NotEmpty;

import java.util.Collection;
import java.util.Map;

public class NotEmptyValidator implements ConstraintValidator<NotEmpty, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        } else if (value instanceof CharSequence s) {
            return !s.isEmpty();
        } else if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        } else if (value instanceof Map<?, ?> m) {
            return !m.isEmpty();
        } else if (value.getClass().isArray()) {
            return ((Object[]) value).length > 0;
        } else {
            throw new UnsupportedOperationException("unable to validate @NotEmpty for " + value);
        }
    }
}