package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.GenericInfo;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.ProviderDependency;

import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProviderParameterLookupCustomizer implements ParameterLookupCustomizer {

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        TypeElement typeElement = context.toTypeElement(variableElement.asType());
        String qualifier = ProcessorUtils.getQualifier(context, variableElement);

        if (variableElement.asType().getKind().isPrimitive()) {
            context.errorMessage("provider injection is impossible for primitive type: " + ProcessorUtils.debugString(variableElement));
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            return doctorRef + ".getProviderOpt(" + typeMirror + ".class, " + qualifier + ").map(" + Provider.class.getCanonicalName() + "::get)";
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            return doctorRef + ".getProvider(" + typeMirror + ".class, " + qualifier + ")";
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            String preamble = doctorRef + ".getProviders(" + typeMirror + ".class, " + qualifier + ")"
                    + ".map(" + Provider.class.getCanonicalName() + "::get)"
                    + ".collect(" + Collectors.class.getCanonicalName();

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Set.class)) {
                return preamble + ".toSet())";
            } else if (ProcessorUtils.isCompatibleWith(context, typeElement, List.class)) {
                return preamble + ".toList())";
            } else if (ProcessorUtils.isCompatibleWith(context, typeElement, Collection.class)) {
                return preamble + ".toList())";
            } else {
                context.errorMessage("unable to inject iterable type: " + typeElement);
                return null;
            }
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            return doctorRef + ".getProviders(" + typeMirror + ".class, " + qualifier + ")";
        }

        if (variableElement.asType().getKind() == TypeKind.ARRAY) {
            String type = typeElement.getQualifiedName().toString();
            return doctorRef + ".getProviders(" + type + ".class, " + qualifier + ").map(" + Provider.class.getCanonicalName() + "::get)" + ".toArray(" + type + "[]::new)";
        }

        return doctorRef + ".getInstance(" + variableElement.asType() + ".class, " + qualifier + ")";
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        TypeElement typeElement = context.toTypeElement(variableElement.asType());
        if (typeElement == null) {
            context.errorMessage("null type from " + ProcessorUtils.debugString(variableElement));
            throw new IllegalArgumentException("null type from " + ProcessorUtils.debugString(variableElement));
        }
        String qualifier = ProcessorUtils.getQualifier(context, variableElement);

        if (variableElement.asType().getKind().isPrimitive()) {
            context.errorMessage("provider injection is impossible for primitive type: " + ProcessorUtils.debugString(variableElement));
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)) {
            return "";
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)
        ) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            return doctorRef + ".getProvider(" + typeMirror + ".class, " + qualifier + ");";
        }

        if (variableElement.asType().getKind() == TypeKind.ARRAY) {
            String type = typeElement.getQualifiedName().toString();
            return doctorRef + ".getProvider(" + type + ".class, " + qualifier + ");";
        }
        return doctorRef + ".getProvider(" + variableElement.asType() + ".class, " + qualifier + ");";
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        TypeElement typeElement = context.toTypeElement(variableElement.asType());
        String qualifier = ProcessorUtils.getQualifier(context, variableElement);

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)) {
            TypeElement type = context.toTypeElement(unwrapJustOne(variableElement.asType()));
            return context.buildDependency(type, qualifier, false);
        }

        if (variableElement.asType().getKind() == TypeKind.ARRAY) {
            return context.buildDependency(typeElement, qualifier, true);
        }
        return context.buildDependency(typeElement, qualifier, true);
    }

    @Override
    public int priority() {
        // always last
        return Integer.MAX_VALUE;
    }

    private static TypeMirror unwrapJustOne(TypeMirror mirror) {
        GenericInfo info = new GenericInfo(mirror);
        if (info.parameterTypes() != null && info.parameterTypes().size() == 1) {
            GenericInfo genericInfo = info.parameterTypes().get(0);
            if (genericInfo.parameterTypes() != null && !genericInfo.parameterTypes().isEmpty()) {
                throw new IllegalArgumentException("can not inject nested parameterized type: " + mirror);
            }
            return genericInfo.type();
        }
        throw new IllegalArgumentException("can not inject type: " + mirror);
    }

}
