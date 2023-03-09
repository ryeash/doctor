package vest.doctor.validation.standard;

import java.util.Arrays;
import java.util.regex.Pattern;

public class PatternValidator extends RegexValidator<jakarta.validation.constraints.Pattern> {
    public PatternValidator(jakarta.validation.constraints.Pattern pattern) {
        super(Pattern.compile(pattern.regexp(), Arrays.stream(pattern.flags()).map(jakarta.validation.constraints.Pattern.Flag::getValue).reduce((a, b) -> a | b).orElse(0)));
    }
}