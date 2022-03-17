package vest.doctor.reactor.http.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.reactor.http.HttpParameterWriter;
import vest.doctor.reactor.http.Param;
import vest.doctor.reactor.http.impl.RouterWriter;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class BodyParameterWriter implements HttpParameterWriter {

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            return RouterWriter.BODY_REF_NAME;
        }
        return null;
    }
}
