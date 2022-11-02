package vest.doctor.http.server.processing;

import jakarta.inject.Inject;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.http.server.HttpParameterWriter;
import vest.doctor.http.server.Param;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BeanParamStringWriter implements HttpParameterWriter {

    private static final List<String> SETTER_PREFIXES = List.of("set", "is", "has");

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Bean.class) != null) {
            return beanParamBuilderCall(context, handlerBuilder, parameter, contextRef) + "(" + contextRef + ")";
        }
        return null;
    }

    private String beanParamBuilderCall(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, String contextRef) {
        TypeMirror typeMirror = parameter.asType();
        String typeWithoutParams = ProcessorUtils.typeWithoutParameters(typeMirror);
        String methodName = "beanParam" + context.nextId();
        MethodBuilder bean = epHandler.newMethod("public static ", typeWithoutParams, " " + methodName + "(RequestContext ", contextRef, ")");

        TypeElement beanType = context.toTypeElement(typeMirror);
        ExecutableElement constructor = injectableConstructor(context, beanType);

        String diamond = Optional.of(beanType.getTypeParameters())
                .filter(l -> !l.isEmpty())
                .map(l -> "<>")
                .orElse("");

        String constructorParams = constructor.getParameters()
                .stream()
                .map(p -> parameterWriting(context, epHandler, p, p, contextRef))
                .collect(Collectors.joining(", ", "(", ")"));

        bean.line(ProcessorUtils.typeWithoutParameters(typeMirror), " bean = new ", ProcessorUtils.typeWithoutParameters(typeMirror), diamond, constructorParams, ";");

        for (VariableElement field : ElementFilter.fieldsIn(beanType.getEnclosedElements())) {
            if (supportedParam(context, field)) {
                ExecutableElement setter = findCorrespondingSetter(context, field, beanType);
                VariableElement setterParameter = setter.getParameters().get(0);
                bean.line("bean.", setter.getSimpleName(), "(", parameterWriting(context, epHandler, setterParameter, field, contextRef), ");");
            }
        }
        for (ExecutableElement method : ElementFilter.methodsIn(beanType.getEnclosedElements())) {
            if (supportedParam(context, method)) {
                if (method.getParameters().size() != 1) {
                    throw new CodeProcessingException("setters in BeanParam objects must have one and only one parameter", method);
                }
                VariableElement setterParameter = method.getParameters().get(0);
                bean.line("bean.", method.getSimpleName(), "(", parameterWriting(context, epHandler, setterParameter, method, contextRef), ");");
            }
        }
        bean.line("return bean;");
        return methodName;
    }

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            throw new CodeProcessingException("body parameters are not allowed in bean parameters");
        }
        for (HttpParameterWriter customization : context.customizations(HttpParameterWriter.class)) {
            String code = customization.writeParameter(context, handlerBuilder, parameter, annotationSource, contextRef);
            if (code != null) {
                return code;
            }
        }
        throw new CodeProcessingException("unable to wire HTTP parameter", parameter);
    }

    private static ExecutableElement injectableConstructor(AnnotationProcessorContext context, TypeElement typeElement) {
        ExecutableElement constructor = null;
        for (ExecutableElement c : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            if (c.getAnnotation(Inject.class) != null) {
                constructor = c;
                break;
            }
            if (c.getParameters().isEmpty()) {
                constructor = c;
            }
        }
        if (constructor == null) {
            throw new CodeProcessingException("failed to find injectable constructor for the BeanParam", typeElement);
        }
        for (VariableElement parameter : constructor.getParameters()) {
            int count = ProcessorUtils.getAnnotationsExtends(context, parameter, Param.class).size();
            if (count != 1) {
                throw new CodeProcessingException("bean constructor parameters must have exactly one request parameter annotation", parameter);
            }
        }
        return constructor;
    }

    private static boolean supportedParam(AnnotationProcessorContext context, Element e) {
        return !ProcessorUtils.getAnnotationsExtends(context, e, Param.class).isEmpty();
    }

    private static ExecutableElement findCorrespondingSetter(AnnotationProcessorContext context, VariableElement field, TypeElement beanType) {
        return ProcessorUtils.allMethods(context, beanType)
                .stream()
                .filter(method -> method.getParameters().size() == 1)
                .filter(method -> {
                    String methodName = method.getSimpleName().toString();
                    return SETTER_PREFIXES.stream()
                            .map(prefix -> prefix + field.getSimpleName())
                            .anyMatch(methodName::equalsIgnoreCase);
                })
                .findFirst()
                .orElseThrow(() -> new CodeProcessingException("missing setter method for BeanParam field", field));
    }
}
