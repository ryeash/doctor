package vest.doctor.validation.standard;

import jakarta.validation.constraints.Past;

public class PastValidator extends DateValidator<Past, Object> {
    public PastValidator() {
        super((epochMillis, now) -> epochMillis < now);
    }
}