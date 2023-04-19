package vest.doctor.validation.standard;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import vest.doctor.AnnotationData;
import vest.doctor.AnnotationMetadata;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.aop.ArgValue;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ValidationAspect implements Aspect {
    private final ProviderRegistry providerRegistry;

    @Inject
    public ValidationAspect(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public Object execute(MethodInvocation methodInvocation) {
        boolean validateAll = validatePresent(methodInvocation.annotationMetadata());
        for (ArgValue argumentValue : methodInvocation.getArgumentValues()) {
            boolean validate = validateAll || validatePresent(argumentValue.type().annotationMetadata());
            if (validate) {
                validateParameter(argumentValue);
            }
        }
        Object o = methodInvocation.next();
        boolean validateReturn = validateAll || validatePresent(methodInvocation.getReturnType().annotationMetadata());
        if (validateReturn) {
            validate(methodInvocation.getReturnType(), o);
        }
        return o;
    }

    private boolean validatePresent(AnnotationMetadata annotationMetadata) {
        return annotationMetadata.findOne(Valid.class).isPresent();
    }

    private void validateParameter(ArgValue argumentValue) {
        validate(argumentValue.type(), argumentValue.get());
    }

    private void validate(TypeInfo typeInfo, Object o) {
        List<AnnotationData> list = typeInfo.annotationMetadata()
                .stream()
                .filter(ad -> ad.type().isAnnotationPresent(Constraint.class))
                .toList();
        List<String> constraintViolations = new LinkedList<>();
        for (AnnotationData ad : list) {
            String message = ad.stringValue("message");
            List<ConstraintValidator<?, Object>> constraintValidators = mapToValidators(ad);
            for (ConstraintValidator<?, Object> constraintValidator : constraintValidators) {
                DoctorConstraintValidatorContext context = new DoctorConstraintValidatorContext(message);
                if (!constraintValidator.isValid(o, context)) {
                    constraintViolations.add(context.getConstraintViolationMessage());
                }
            }
        }
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(String.join("; ", constraintViolations), Collections.emptySet());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<ConstraintValidator<?, Object>> mapToValidators(AnnotationData annotationData) {
        Constraint con = annotationData.type().getAnnotation(Constraint.class);
        Class<? extends ConstraintValidator<?, ?>>[] classes = con.validatedBy();
        if (classes != null && classes.length > 0) {
            return (List) customConstraints(annotationData);
        } else {
            return List.of(builtInConstraint(annotationData));
        }
    }

    private List<ConstraintValidator<?, ?>> customConstraints(AnnotationData annotationData) {
        Constraint con = annotationData.type().getAnnotation(Constraint.class);
        Class<? extends ConstraintValidator<?, ?>>[] classes = con.validatedBy();
        return Arrays.stream(classes)
                .map(providerRegistry::getInstance)
                .peek(v -> v.initialize(null)) // TODO: NPE going to happen
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"rawtypes"})
    private ConstraintValidator builtInConstraint(AnnotationData annotationData) {
        Class<? extends Annotation> constraint = annotationData.type();
        if (constraint.isInstance(AssertFalse.class)) {
            return new AssertFalseValidator();
        } else if (constraint.isInstance(AssertTrue.class)) {
            return new AssertTrueValidator();
        } else if (constraint.isInstance(DecimalMax.class)) {
            BigDecimal compare = new BigDecimal(annotationData.stringValue("value"));
            boolean inclusive = annotationData.booleanValue("inclusive");
            return new DecimalMaxValidator(compare, inclusive);
        } else if (constraint.isInstance(DecimalMin.class)) {
            BigDecimal compare = new BigDecimal(annotationData.stringValue("value"));
            boolean inclusive = annotationData.booleanValue("inclusive");
            return new DecimalMinValidator(compare, inclusive);
        } else if (constraint.isInstance(Digits.class)) {
            int integer = annotationData.intValue("integer");
            int fraction = annotationData.intValue("fraction");
            return new DigitsValidator(integer, fraction);
        } else if (constraint.isInstance(Email.class)) {
            return new EmailValidator();
        } else if (constraint.isInstance(Future.class)) {
            return new FutureValidator();
        } else if (constraint.isInstance(FutureOrPresent.class)) {
            return new FutureOrPresentValidator();
        } else if (constraint.isInstance(Max.class)) {
            long value = annotationData.longValue("value");
            return new MaxValidator(BigDecimal.valueOf(value));
        } else if (constraint.isInstance(Min.class)) {
            long value = annotationData.longValue("value");
            return new MinValidator(BigDecimal.valueOf(value));
        } else if (constraint.isInstance(Negative.class)) {
            return new NegativeValidator();
        } else if (constraint.isInstance(NegativeOrZero.class)) {
            return new NegativeOrZeroValidator();
        } else if (constraint.isInstance(NotBlank.class)) {
            return new NotBlankValidator();
        } else if (constraint.isInstance(NotEmpty.class)) {
            return new NotEmptyValidator();
        } else if (constraint.isInstance(NotNull.class)) {
            return new NotNullValidator();
        } else if (constraint.isInstance(Null.class)) {
            return new NotNullValidator();
        } else if (constraint.isInstance(Past.class)) {
            return new PastValidator();
        } else if (constraint.isInstance(Pattern.class)) {
            String regexp = annotationData.stringValue("regexp");
            int flags = annotationData.enumArrayValue("flags").stream().map(e -> ((Pattern.Flag) e).getValue()).reduce((a, b) -> a | b).orElse(0);
            return new PatternValidator(regexp, flags);
        } else if (constraint.isInstance(Positive.class)) {
            return new PositiveValidator();
        } else if (constraint.isInstance(PositiveOrZero.class)) {
            return new PositiveOrZeroValidator();
        } else if (constraint.isInstance(Size.class)) {
            int min = annotationData.intValue("min");
            int max = annotationData.intValue("max");
            return new SizeValidator(min, max);
        } else {
            throw new UnsupportedOperationException("unsupported constraint " + constraint);
        }
    }
}
