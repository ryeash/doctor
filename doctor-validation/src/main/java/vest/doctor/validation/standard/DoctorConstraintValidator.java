package vest.doctor.validation.standard;

import jakarta.validation.ValidationException;

@FunctionalInterface
public interface DoctorConstraintValidator<T> {

    ValidationException validate(T value);
}
