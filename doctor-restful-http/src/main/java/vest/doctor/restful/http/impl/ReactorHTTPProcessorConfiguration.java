package vest.doctor.restful.http.impl;

import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;
import vest.doctor.restful.http.processing.AttributeParameterWriter;
import vest.doctor.restful.http.processing.BeanParamStringWriter;
import vest.doctor.restful.http.processing.BodyParameterWriter;
import vest.doctor.restful.http.processing.ContextValuesParameterWriter;
import vest.doctor.restful.http.processing.CookieParamStringWriter;
import vest.doctor.restful.http.processing.HeaderParamStringWriter;
import vest.doctor.restful.http.processing.PathParamStringWriter;
import vest.doctor.restful.http.processing.ProvidedParameterWriter;
import vest.doctor.restful.http.processing.QueryParamStringWriter;

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
