package vest.doctor.validation.standard;

import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

public class DecimalMaxValidator extends NumberValidator<DecimalMax> {
    public DecimalMaxValidator(BigDecimal max, boolean inclusive) {
        super(max, inclusive
                ? (a, b) -> a.compareTo(b) <= 0
                : (a, b) -> a.compareTo(b) < 0);
    }
}