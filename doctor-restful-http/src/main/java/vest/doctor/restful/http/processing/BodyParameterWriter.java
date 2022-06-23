package vest.doctor.restful.http.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.restful.http.HttpParameterWriter;
import vest.doctor.restful.http.Param;
import vest.doctor.restful.http.impl.OrchestrationWriter;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class BodyParameterWriter implements HttpParameterWriter {

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            return OrchestrationWriter.BODY_REF_NAME;
        }
        return null;
    }
}
