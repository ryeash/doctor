package vest.doctor.processor;

import doctor.processor.ProcessorUtils;
import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.ProviderDependency;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.stream.Stream;

import static doctor.processor.ProcessorUtils.unwrapJustOne;

public class ProviderParameterLookupCustomizer implements ParameterLookupCustomizer {

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        return ProcessorUtils.providerLookupCode(context, variableElement, providerRegistryRef);
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
            return ProcessorUtils.getProviderCode(typeMirror.toString(), qualifier) + ";";
        }

        if (variableElement.asType().getKind() == TypeKind.ARRAY) {
            return ProcessorUtils.getProviderCode(typeElement.getQualifiedName().toString(), qualifier) + ";";
        }
        return ProcessorUtils.getProviderCode(variableElement.asType().toString(), qualifier) + ";";
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


}
