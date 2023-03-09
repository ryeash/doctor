package vest.doctor.validation.standard;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import vest.doctor.AnnotationData;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.aop.ArgValue;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.util.Arrays;
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
        for (ArgValue argumentValue : methodInvocation.getArgumentValues()) {
            validateParameter(argumentValue);
        }
        Object o = methodInvocation.next();
        validateReturnValue(methodInvocation.getReturnType(), o);
        return o;
    }

    private void validateParameter(ArgValue argumentValue) {
        validateReturnValue(argumentValue.type(), argumentValue.get());
    }

    private void validateReturnValue(TypeInfo typeInfo, Object o) {
        typeInfo.annotationMetadata()
                .stream()
                .filter(ad -> ad.type().isAnnotationPresent(Constraint.class))
                .map(this::mapToValidators)
                .flatMap(List::stream)
                .forEach(v -> {
                    v.isValid(o, null);
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<ConstraintValidator> mapToValidators(AnnotationData annotationData) {
        Constraint con = annotationData.type().getAnnotation(Constraint.class);
        Class<? extends ConstraintValidator<?, ?>>[] classes = con.validatedBy();
        if (classes != null && classes.length > 0) {
            return customConstraints(annotationData);
        } else {
            return List.of(builtInConstraint(annotationData));
        }
    }

    @SuppressWarnings({"rawtypes"})
    private List<ConstraintValidator> customConstraints(AnnotationData annotationData) {
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
            return new DecimalMaxValidator();
        } else if (constraint.isInstance(DecimalMin.class)) {
            return new DecimalMinValidator();
        } else if (constraint.isInstance(Digits.class)) {
            return new DigitsValidator();
        } else if (constraint.isInstance(Email.class)) {
            return new EmailValidator();
        } else {
            throw new UnsupportedOperationException("unknown constraint " + constraint);
        }
    }
}
