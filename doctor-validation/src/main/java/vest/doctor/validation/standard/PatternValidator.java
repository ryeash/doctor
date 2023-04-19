package vest.doctor.validation.standard;

import java.util.Arrays;
import java.util.regex.Pattern;

public class PatternValidator extends RegexValidator<jakarta.validation.constraints.Pattern> {
    public PatternValidator(String regexp, int flags) {
        super(Pattern.compile(regexp, flags));
    }
}