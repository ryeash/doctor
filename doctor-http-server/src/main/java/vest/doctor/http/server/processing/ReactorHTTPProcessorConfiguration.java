package vest.doctor.http.server.processing;

import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class ReactorHTTPProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return List.of(new HandlerWriter(),
                new HttpStringConverter(),

                new ContextValuesParameterWriter(),
                new BodyParameterWriter(),
                new AttributeParameterWriter(),
                new ProvidedParameterWriter(),
                new BeanParamStringWriter(),
                new PathParamStringWriter(),
                new QueryParamStringWriter(),
                new HeaderParamStringWriter(),
                new CookieParamStringWriter());
    }
}
