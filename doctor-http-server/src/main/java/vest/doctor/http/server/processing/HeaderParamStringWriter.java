package vest.doctor.http.server.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.Param;
import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class HeaderParamStringWriter extends AbstractBasicStringTypeWriter<Param.Header> {
    public HeaderParamStringWriter() {
        super(Param.Header.class);
    }

    @Override
    protected String getParam(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        String name = AbstractBasicStringTypeWriter.getParamName(annotationSource, parameter, Param.Header.class, Param.Header::value);
        return contextRef + ".request().header(\"" + name + "\")";
    }
}
