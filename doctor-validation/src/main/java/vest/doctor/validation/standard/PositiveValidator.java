package vest.doctor.validation.standard;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PositiveValidator extends NumberValidator<Positive> {
    private PositiveValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.compareTo(BigDecimal.ZERO) > 0);
    }
}