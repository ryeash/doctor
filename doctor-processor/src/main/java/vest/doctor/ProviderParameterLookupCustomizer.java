package vest.doctor;

import javax.lang.model.element.VariableElement;

public class ProviderParameterLookupCustomizer implements ParameterLookupCustomizer {
    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        VariableElementMetadata metadata = new VariableElementMetadata(context, variableElement);
        if (metadata.primitive) {
            context.errorMessage("provider injection impossible for primitive type: " + ProcessorUtils.debugString(variableElement));
        }
        if (metadata.optional && metadata.provider) {
            context.errorMessage("injected parameters may not be both optional and provider types: " + ProcessorUtils.debugString(variableElement));
        }
        if (metadata.optional) {
            return doctorRef + ".getProviderOpt(" + metadata.type.asType() + ".class, " + metadata.qualifier + ").map(javax.inject.Provider::get)";
        } else {
            return doctorRef + ".getProvider(" + metadata.type.asType() + ".class, " + metadata.qualifier + ")" + (metadata.provider ? "" : ".get()");
        }
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        VariableElementMetadata metadata = new VariableElementMetadata(context, variableElement);
        if (metadata.optional) {
            return "";
        }
        return doctorRef + ".getProvider(" + metadata.type.asType() + ".class, " + metadata.qualifier + ");";
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        VariableElementMetadata metadata = new VariableElementMetadata(context, variableElement);
        return context.buildDependency(metadata.type, metadata.qualifier, !metadata.optional);
    }

    @Override
    public int priority() {
        // always last
        return Integer.MAX_VALUE;
    }

}
