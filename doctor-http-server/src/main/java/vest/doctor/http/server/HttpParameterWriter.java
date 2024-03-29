package vest.doctor.http.server;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CustomizationPoint;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * A customization point that creates the code to wire request params into endpoint method calls.
 */
public interface HttpParameterWriter extends CustomizationPoint {

    /**
     * Create the code to pull the value for a parameter from the request context.
     * The generated code must be suitable for putting inline into a method call, i.e. no semicolon.
     *
     * @param context          the processing context
     * @param handlerBuilder   the class builder for the endpoint handler container
     * @param parameter        the parameter to wire
     * @param annotationSource the source to get annotations from
     * @param contextRef       the name to use when referencing the {@link RequestContext} in the generated code
     * @return the code to wire the parameter value
     */
    String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef);
}
