package vest.doctor.validation.standard;

import jakarta.validation.constraints.FutureOrPresent;

public class FutureOrPresentValidator extends DateValidator<FutureOrPresent, Object> {
    public FutureOrPresentValidator() {
        super((epochMillis, now) -> epochMillis >= now);
    }
}