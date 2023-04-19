package vest.doctor.validation.standard;

import jakarta.validation.constraints.NegativeOrZero;

import java.math.BigDecimal;

public class NegativeOrZeroValidator extends NumberValidator<NegativeOrZero> {
    public NegativeOrZeroValidator() {
        super(BigDecimal.ZERO, (a, b) -> a.compareTo(b) <= 0);
    }
}