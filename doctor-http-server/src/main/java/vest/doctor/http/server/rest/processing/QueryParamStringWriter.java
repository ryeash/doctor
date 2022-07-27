package vest.doctor.http.server.rest.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.rest.Param;
import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class QueryParamStringWriter extends AbstractBasicStringTypeWriter<Param.Query> {
    public QueryParamStringWriter() {
        super(Param.Query.class);
    }

    @Override
    protected String getParam(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        String name = AbstractBasicStringTypeWriter.getParamName(annotationSource, parameter, Param.Query.class, Param.Query::value);
        return contextRef + ".request().queryParam(\"" + name + "\")";
    }
}
