package vest.doctor.validation.standard;

import jakarta.validation.constraints.Max;

import java.math.BigDecimal;

public class MaxValidator extends NumberValidator<Max> {
    public MaxValidator(BigDecimal compareTo) {
        super(compareTo, (a, b) -> a.longValue() <= b.longValue());
    }
}