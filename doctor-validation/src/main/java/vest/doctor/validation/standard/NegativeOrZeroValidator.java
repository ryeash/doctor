package vest.doctor.validation.standard;

import jakarta.validation.constraints.NegativeOrZero;

import java.math.BigDecimal;

public class NegativeOrZeroValidator extends NumberValidator<NegativeOrZero> {
    private NegativeOrZeroValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.compareTo(BigDecimal.ZERO) <= 0);
    }
}