package vest.doctor.validation.standard;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PositiveValidator extends NumberValidator<Positive> {
    public PositiveValidator() {
        super(BigDecimal.ZERO, (a, b) -> a.compareTo(b) > 0);
    }
}