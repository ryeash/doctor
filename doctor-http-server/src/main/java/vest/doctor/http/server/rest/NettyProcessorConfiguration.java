package vest.doctor.http.server.rest;

import vest.doctor.CustomizationPoint;
import vest.doctor.ProcessorConfiguration;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class NettyProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return List.of(new EndpointLoaderWriter(), new HttpStringConverter());
    }
}
