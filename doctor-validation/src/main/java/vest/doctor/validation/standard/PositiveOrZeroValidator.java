package vest.doctor.validation.standard;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class PositiveOrZeroValidator extends NumberValidator<PositiveOrZero> {
    public PositiveOrZeroValidator() {
        super(BigDecimal.ZERO, (a, b) -> a.compareTo(b) >= 0);
    }
}