package vest.doctor.validation.standard;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public class MinValidator extends NumberValidator<Min> {
    private MinValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.longValue() >= b.longValue());
    }
}