package vest.doctor.validation.standard;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class PositiveOrZeroValidator extends NumberValidator<PositiveOrZero> {
    private PositiveOrZeroValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.compareTo(BigDecimal.ZERO) >= 0);
    }
}