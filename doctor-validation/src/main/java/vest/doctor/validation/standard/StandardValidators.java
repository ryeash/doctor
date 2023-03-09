package vest.doctor.validation.standard;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import vest.doctor.AnnotationData;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class StandardValidators {

    private static final Map<Class<? extends Annotation>, DoctorConstraintValidator<?, Object>> STANDARD_MAPPING = new LinkedHashMap<>();

    static {
        STANDARD_MAPPING.put(AssertFalse.class, StandardValidators::assertFalse);
        STANDARD_MAPPING.put(AssertTrue.class, StandardValidators::assertTrue);
    }

    public static ValidationException validate(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        DoctorConstraintValidator<?, Object> validator = STANDARD_MAPPING.get(annotationData.type());
        if (validator == null) {
            throw new RuntimeException("invalid constraint annotation " + annotationData + " no validator registered");
        }
        return validator.validate(annotationData, value, ctx);
    }

    public static ValidationException assertFalse(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        return Optional.ofNullable(value)
                .map(toType(Boolean.class))
                .map(bool -> !bool)
                .map(checkValid(annotationData))
                .orElse(null);
    }

    public static ValidationException assertTrue(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        return Optional.ofNullable(value)
                .map(toType(Boolean.class))
                .map(checkValid(annotationData))
                .orElse(null);
    }

    public static ValidationException decimalMax(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        BigDecimal max = new BigDecimal(annotationData.stringValue("value"));
        boolean inclusive = annotationData.booleanValue("inclusive");
        return Optional.ofNullable(value)
                .map(StandardValidators::convertNumber)
                .map(number -> {
                    int c = number.compareTo(max);
                    return inclusive ? c <= 0 : c < 0;
                })
                .map(checkValid(annotationData))
                .orElse(null);
    }

    public static ValidationException decimalMin(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        BigDecimal min = new BigDecimal(annotationData.stringValue("value"));
        boolean inclusive = annotationData.booleanValue("inclusive");
        return Optional.ofNullable(value)
                .map(StandardValidators::convertNumber)
                .map(number -> {
                    int c = number.compareTo(min);
                    return inclusive ? c >= 0 : c > 0;
                })
                .map(checkValid(annotationData))
                .orElse(null);
    }

    public static ValidationException digits(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {
        int integer = annotationData.intValue("integer");
        int fraction = annotationData.intValue("fraction");
        return Optional.ofNullable(value)
                .map(StandardValidators::convertNumber)
                .map(number -> {
                    BigDecimal n = new BigDecimal(value.toString());
                    int beforeDecimal = n.signum() == 0 ? 1 : n.precision() - n.scale();
                    return beforeDecimal <= integer && n.scale() <= fraction;
                })
                .map(checkValid(annotationData))
                .orElse(null);
    }

    public static ValidationException pattern(AnnotationData annotationData, Object value, ConstraintValidatorContext ctx) {

        int integer = annotationData.intValue("integer");
        int fraction = annotationData.intValue("fraction");
        return Optional.ofNullable(value)
                .map(StandardValidators::convertNumber)
                .map(number -> {
                    BigDecimal n = new BigDecimal(value.toString());
                    int beforeDecimal = n.signum() == 0 ? 1 : n.precision() - n.scale();
                    return beforeDecimal <= integer && n.scale() <= fraction;
                })
                .map(checkValid(annotationData))
                .orElse(null);
    }

    private static <T> Function<Object, T> toType(Class<T> type) {
        return o -> {
            if (type.isInstance(o)) {
                return type.cast(o);
            } else {
                throw new ValidationException("invalid value, expected " + type + ", got " + o);
            }
        };
    }

    private static BigDecimal convertNumber(Object o) {
        if (o instanceof BigDecimal bd) {
            return bd;
        } else if (o instanceof BigInteger bi) {
            return new BigDecimal(bi);
        } else if (o instanceof Number n) {
            return new BigDecimal(n.toString());
        } else if (o instanceof CharSequence s) {
            return new BigDecimal(s.toString());
        } else {
            throw new ValidationException("invalid value, expected a number, got " + o);
        }
    }

    private static Function<Boolean, ValidationException> checkValid(AnnotationData annotationData) {
        return success -> {
            if (!success) {
                String failureMessage = annotationData != null ? annotationData.stringValue("message") : "validation failure";
                return new ValidationException(failureMessage);
            } else {
                return null;
            }
        };
    }
}
