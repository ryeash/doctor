package vest.doctor;

public interface NewInstanceCustomizer extends CustomizationPoint, Prioritized {

    void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String doctorRef);
}
