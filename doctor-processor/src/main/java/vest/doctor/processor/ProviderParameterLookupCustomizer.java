package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ParameterLookupCustomizer;
import vest.doctor.processing.ProviderDependency;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.stream.Stream;

import static vest.doctor.codegen.ProcessorUtils.unwrapJustOne;

public class ProviderParameterLookupCustomizer implements ParameterLookupCustomizer {

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        return ProcessorUtils.providerLookupCode(context, variableElement, providerRegistryRef);
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        TypeMirror typeMirror = variableElement.asType();
        String qualifier = ProcessorUtils.getQualifier(context, variableElement);
        if (ProcessorUtils.isCompatibleWith(context, typeMirror, Optional.class)
            || ProcessorUtils.isCompatibleWith(context, typeMirror, Iterable.class)
            || ProcessorUtils.isCompatibleWith(context, typeMirror, Stream.class)
            || ProcessorUtils.isCompatibleWith(context, typeMirror, ProviderRegistry.class)
            || variableElement.asType().getKind() == TypeKind.ARRAY) {
            return "";
        }
        if (ProcessorUtils.isCompatibleWith(context, typeMirror, Provider.class)) {
            return ProcessorUtils.getProviderCode(unwrapJustOne(variableElement.asType()), qualifier) + ";";
        }
        return ProcessorUtils.getProviderCode(variableElement.asType(), qualifier) + ";";
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
        return Prioritized.LOWEST_PRIORITY;
    }
}
