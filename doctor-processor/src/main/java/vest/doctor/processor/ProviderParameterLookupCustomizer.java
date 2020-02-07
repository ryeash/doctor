package vest.doctor.processor;

import doctor.processor.GenericInfo;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
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
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        try {
            TypeElement typeElement = context.toTypeElement(variableElement.asType());
            String qualifier = ProcessorUtils.getQualifier(context, variableElement);

            if (variableElement.asType().getKind().isPrimitive()) {
                context.errorMessage("provider injection is impossible for primitive type: " + ProcessorUtils.debugString(variableElement));
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                return providerRegistryRef + ".getProviderOpt(" + typeMirror + ".class, " + qualifier + ").map(" + Provider.class.getCanonicalName() + "::get)";
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                return providerRegistryRef + ".getProvider(" + typeMirror + ".class, " + qualifier + ")";
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                String preamble = providerRegistryRef + ".getProviders(" + typeMirror + ".class, " + qualifier + ")"
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
                return providerRegistryRef + ".getProviders(" + typeMirror + ".class, " + qualifier + ")";
            }

            if (variableElement.asType().getKind() == TypeKind.ARRAY) {
                String type = typeElement.getQualifiedName().toString();
                return providerRegistryRef + ".getProviders(" + type + ".class, " + qualifier + ").map(" + Provider.class.getCanonicalName() + "::get)" + ".toArray(" + type + "[]::new)";
            }

            return providerRegistryRef + ".getInstance(" + variableElement.asType() + ".class, " + qualifier + ")";
        } catch (IllegalArgumentException e) {
            context.errorMessage("error wiring parameter: " + e.getMessage() + ": " + ProcessorUtils.debugString(variableElement));
            throw e;
        }
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        TypeElement typeElement = context.toTypeElement(variableElement.asType());
        String qualifier = ProcessorUtils.getQualifier(context, variableElement);
        if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)) {
            return "";
        }

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)
        ) {
            TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
            return providerRegistryRef + ".getProvider(" + typeMirror + ".class, " + qualifier + ");";
        }

        if (variableElement.asType().getKind() == TypeKind.ARRAY) {
            String type = typeElement.getQualifiedName().toString();
            return providerRegistryRef + ".getProvider(" + type + ".class, " + qualifier + ");";
        }
        return providerRegistryRef + ".getProvider(" + variableElement.asType() + ".class, " + qualifier + ");";
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
