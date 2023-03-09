package vest.doctor.validation.standard;

import jakarta.validation.constraints.PastOrPresent;

public class PastOrPresentValidator extends DateValidator<PastOrPresent, Object> {
    public PastOrPresentValidator() {
        super((epochMillis, now) -> epochMillis <= now);
    }
}