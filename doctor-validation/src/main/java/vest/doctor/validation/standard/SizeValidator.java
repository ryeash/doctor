package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Map;

public class SizeValidator implements ConstraintValidator<Size, Object> {
    private final int min;
    private final int max;

    public SizeValidator(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int size;
        if (value instanceof CharSequence s) {
            size = s.length();
        } else if (value instanceof Collection<?> c) {
            size = c.size();
        } else if (value instanceof Map<?, ?> m) {
            size = m.size();
        } else if (value.getClass().isArray()) {
            size = ((Object[]) value).length;
        } else {
            throw new UnsupportedOperationException("unable to validate @NotEmpty for " + value);
        }
        return min <= size && size <= max;
    }
}