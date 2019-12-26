package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ClassBuilder;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;

import javax.lang.model.element.ExecutableElement;
import java.util.Optional;

public class RouterWriter implements ProviderCustomizationPoint {

    private ClassBuilder routerBuilder = new ClassBuilder();

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                .map(Path::value)
                .orElse(new String[]{"/"});
        for (ExecutableElement method : providerDefinition.methods()) {

        }

        // unchanged
        return providerRef;
    }

    @Override
    public void finish(AnnotationProcessorContext context) {

    }
}
