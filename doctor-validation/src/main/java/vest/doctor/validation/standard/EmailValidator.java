package vest.doctor.validation.standard;

import jakarta.validation.constraints.Email;

import java.util.regex.Pattern;

public class EmailValidator extends RegexValidator<Email> {
    private static final Pattern EMAIL_REGEX = Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

    public EmailValidator() {
        super(EMAIL_REGEX);
    }
}