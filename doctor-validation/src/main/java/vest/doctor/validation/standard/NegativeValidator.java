package vest.doctor.validation.standard;

import jakarta.validation.constraints.Negative;

import java.math.BigDecimal;

public class NegativeValidator extends NumberValidator<Negative> {
    public NegativeValidator() {
        super(BigDecimal.ZERO, (a, b) -> a.compareTo(b) < 0);
    }
}