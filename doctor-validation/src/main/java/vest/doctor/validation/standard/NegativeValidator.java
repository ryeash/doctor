package vest.doctor.validation.standard;

import jakarta.validation.constraints.Negative;

import java.math.BigDecimal;

public class NegativeValidator extends NumberValidator<Negative> {
    private NegativeValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.compareTo(BigDecimal.ZERO) < 0);
    }
}