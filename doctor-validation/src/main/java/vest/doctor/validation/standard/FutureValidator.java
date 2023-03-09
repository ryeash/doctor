package vest.doctor.validation.standard;

import jakarta.validation.constraints.Future;

public class FutureValidator extends DateValidator<Future, Object> {
    public FutureValidator() {
        super((epochMillis, now) -> epochMillis > now);
    }
}
