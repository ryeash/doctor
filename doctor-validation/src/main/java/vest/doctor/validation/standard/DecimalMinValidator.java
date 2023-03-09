package vest.doctor.validation.standard;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public class DecimalMinValidator extends NumberValidator<DecimalMin> {
    public DecimalMinValidator(BigDecimal max, boolean inclusive) {
        super(max, inclusive
                ? (a, b) -> a.compareTo(b) >= 0
                : (a, b) -> a.compareTo(b) > 0);
    }
}