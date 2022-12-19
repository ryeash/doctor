package vest.doctor.http.server.processing;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import jakarta.inject.Provider;
import vest.doctor.AnnotationMetadata;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.http.server.BodyInterchange;
import vest.doctor.http.server.Endpoint;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpMethod;
import vest.doctor.http.server.HttpParameterWriter;
import vest.doctor.http.server.Param;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.impl.HttpException;
import vest.doctor.http.server.impl.Router;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.reactive.Rx;
import vest.doctor.runtime.AnnotationDataImpl;
import vest.doctor.runtime.AnnotationMetadataImpl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class HandlerWriter implements ProviderDefinitionListener {
    public static final String BODY_REF_NAME = "body";
    public static final String REQUEST_CONTEXT_REF = "requestContext";
    private static final String[] EMPTY_ARR = new String[0];
    private static final String[] ROOT = new String[]{"/"};
    private final Set<ExecutableElement> processedMethods = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Endpoint.class) == null) {
            return;
        }
        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Endpoint.class))
                .map(Endpoint::value)
                .orElse(ROOT);

        for (ExecutableElement m : ProcessorUtils.allMethods(context, providerDefinition.providedType())) {
            if (m.getModifiers().contains(Modifier.PUBLIC) && processedMethods.add(m)) {
                List<String> httpMethods = getMethods(m);
                if (!httpMethods.isEmpty()) {
                    String[] paths = Optional.ofNullable(m.getAnnotation(Endpoint.class))
                            .map(Endpoint::value)
                            .orElse(EMPTY_ARR);
                    initWiredHandler(context, providerDefinition, m, httpMethods, paths(roots, paths));
                } else if (m.getAnnotation(Endpoint.class) != null) {
                    throw new CodeProcessingException("http method is required for endpoints, e.g. @GET", m);
                }
            }
        }
    }

    public void initWiredHandler(AnnotationProcessorContext context,
                                 ProviderDefinition providerDefinition,
                                 ExecutableElement executableElement,
                                 List<String> methods,
                                 List<String> paths) {
        String className = providerDefinition.providedType() + "$" + executableElement.getSimpleName() + "$Handler" + context.nextId();
        ClassBuilder handler = new ClassBuilder()
                .setClassName(className)
                .addImportClass(Map.class)
                .addImportClass(List.class)
                .addImportClass(Optional.class)
                .addImportClass(AnnotationMetadata.class)
                .addImportClass(AnnotationMetadataImpl.class)
                .addImportClass(AnnotationDataImpl.class)
                .addImportClass(Cookie.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(providerDefinition.providedType())
                .addImportClass(Router.class)
                .addImportClass(BodyInterchange.class)
                .addImportClass(HttpException.class)
                .addImportClass(HttpResponseStatus.class)
                .addImportClass(RequestContext.class)
                .addImportClass(Response.class)
                .addImportClass(Request.class)
                .addImportClass(Handler.class)
                .addImportClass(Rx.class)
                .addImportClass(Flow.class)
                .addImportClass(Flow.Publisher.class)
                .addImplementsInterface(GeneratedHandler.class)
                .addField("private ProviderRegistry providerRegistry")
                .addField("private BodyInterchange bodyInterchange")
                .addField("private Provider<" + providerDefinition.providedType() + "> provider");

        MethodBuilder init = handler.newMethod("@Override public void init(ProviderRegistry providerRegistry, Router router, BodyInterchange bodyInterchange)");
        init.line("this.providerRegistry = providerRegistry;");
        init.line("this.bodyInterchange = bodyInterchange;");
        init.line("this.provider = ", ProcessorUtils.getProviderCode(providerDefinition), ";");
        for (String method : methods) {
            for (String path : paths) {
                init.line("router.route(",
                        ProcessorUtils.escapeAndQuoteStringForCode(method), ',',
                        ProcessorUtils.escapeAndQuoteStringForCode(path), ',',
                        "this);");
            }
        }

        MethodBuilder handlerBuilder = handler.newMethod("@Override public Flow.Publisher<Response> handle(final RequestContext " + REQUEST_CONTEXT_REF + ") throws Exception");
        isValidMethod(context, executableElement);
        GenericInfo returnTypeInfo = new GenericInfo(executableElement, executableElement.getReturnType());
        String returnTypeInfoRef = "returnType_" + executableElement.getSimpleName() + context.nextId();
        handler.addField("private static final TypeInfo " + returnTypeInfoRef + " = ", returnTypeInfo.newTypeInfo(context));

        boolean bodyIsPublisher = false;
        VariableElement bodyElement = bodyParameter(executableElement);
        TypeMirror bodyParam = bodyElement != null ? bodyElement.asType() : null;
        String bodyType;
        if (bodyParam != null) {
            GenericInfo genericInfo = new GenericInfo(bodyParam);
            bodyIsPublisher = ProcessorUtils.isCompatibleWith(context, genericInfo.type(), Flow.Publisher.class);
            bodyType = "bodyType_" + executableElement.getSimpleName() + context.nextId();
            handler.addField("private static final TypeInfo ", bodyType, "=", new GenericInfo(bodyElement).newTypeInfo(context));
        } else {
            bodyType = "null";
        }
        String parameters = executableElement.getParameters().stream()
                .map(p -> parameterWriting(context, handler, p, p))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "provider.get()." + executableElement.getSimpleName() + parameters;

        if (bodyIsPublisher) {
            handlerBuilder.line(bodyParam, " ", BODY_REF_NAME, "=bodyInterchange.read(requestContext,", bodyType, ");");
            handlerBuilder.line(executableElement.getReturnType().toString(), " result = ", callMethod, ';');
            handlerBuilder.line("return bodyInterchange.write(", REQUEST_CONTEXT_REF, ", " + returnTypeInfoRef + ", result);");
        } else {
            String bodyTypeString = bodyParam != null ? bodyParam.toString() : "Object";
            handlerBuilder.line("return Rx.<", bodyTypeString, ">from(bodyInterchange.read(", REQUEST_CONTEXT_REF, ", ", bodyType, "))");
            handlerBuilder.line(".onNext((" + BODY_REF_NAME + ", subscription, subscriber) -> {");
            handlerBuilder.line("subscriber.onNext(" + callMethod + ");");
            handlerBuilder.line("})");
            handlerBuilder.line(".mapPublisher(result -> bodyInterchange.write(requestContext, " + returnTypeInfoRef + ", result));");
        }

        context.addServiceImplementation(GeneratedHandler.class, className);
        handler.writeClass(context.filer());
    }

    private static List<String> paths(String[] roots, String[] paths) {
        List<String> allPaths = new LinkedList<>();
        for (String root : roots) {
            if (paths.length == 0) {
                allPaths.add(squeeze("/" + root, '/'));
            } else {
                for (String path : paths) {
                    allPaths.add(squeeze("/" + root + "/" + path, '/'));
                }
            }
        }
        return allPaths;
    }

    public static String squeeze(String s, char c) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.length() == 1 && s.charAt(0) == c) {
            return s;
        }
        char[] a = s.toCharArray();
        int n = 1;
        for (int i = 1; i < a.length; i++) {
            a[n] = a[i];
            if (a[n] != c) {
                n++;
            } else if (a[n - 1] != c) {
                n++;
            }
        }
        return new String(a, 0, n);
    }

    private void isValidMethod(AnnotationProcessorContext context, ExecutableElement method) {
        // all parameters have one and only one param annotation
        for (VariableElement parameter : method.getParameters()) {
            int count = ProcessorUtils.getAnnotationsExtends(context, parameter, Param.class).size();
            if (count != 1) {
                throw new CodeProcessingException("http endpoint parameters must have exactly one parameter annotation", parameter);
            }
        }

        // zero or one @Body parameters
        long bodyParams = method.getParameters().stream().filter(v -> v.getAnnotation(Param.Body.class) != null).count();
        if (bodyParams > 1) {
            throw new CodeProcessingException("only one parameter may be marked with @Body in an endpoint method", method);
        }

        // void not allowed
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            throw new CodeProcessingException("the return type for endpoint methods may not be void", method);
        }

        // must be more specific than 'Object'
        if (method.getReturnType().toString().equalsIgnoreCase(Object.class.getCanonicalName())) {
            throw new CodeProcessingException("endpoint methods must declare specific return types (not Object)", method);
        }
    }

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource) {
        for (HttpParameterWriter customization : context.customizations(HttpParameterWriter.class)) {
            String code = customization.writeParameter(context, handlerBuilder, parameter, annotationSource, REQUEST_CONTEXT_REF);
            if (code != null) {
                return code;
            }
        }
        throw new CodeProcessingException("unsupported parameter - no HttpParameterWriter is registered to handle", parameter);
    }

    private static VariableElement bodyParameter(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .filter(m -> m.getAnnotation(Param.Body.class) != null)
                .findFirst()
                .orElse(null);
    }

    private static List<String> getMethods(ExecutableElement method) {
        return method.getAnnotationMirrors()
                .stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(at -> at.asElement().getAnnotation(HttpMethod.class))
                .filter(Objects::nonNull)
                .map(HttpMethod::value)
                .toList();
    }
}
