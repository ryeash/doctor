package vest.doctor.validation.standard;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;

public class DoctorConstraintValidatorContext implements ConstraintValidatorContext {

    private static final ClockProvider CLOCK_PROVIDER = new ClockProviderImpl();

    private final String defaultMessage;

    public DoctorConstraintValidatorContext(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    @Override
    public void disableDefaultConstraintViolation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultConstraintMessageTemplate() {
        return defaultMessage;
    }

    @Override
    public ClockProvider getClockProvider() {
        return CLOCK_PROVIDER;
    }

    @Override
    public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return null;
    }

    public String getConstraintViolationMessage() {
        return defaultMessage;
    }

    private static final class ClockProviderImpl implements ClockProvider {

        @Override
        public Clock getClock() {
            return Clock.systemUTC();
        }
    }
}
