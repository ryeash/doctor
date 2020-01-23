package vest.doctor;

public interface ProviderCustomizationPoint extends CustomizationPoint, Prioritized {

    String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String beanProviderRef);
}
