package vest.doctor.http.server.rest.processing;

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
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.impl.HttpException;
import vest.doctor.http.server.impl.Router;
import vest.doctor.http.server.rest.BodyInterchange;
import vest.doctor.http.server.rest.Endpoint;
import vest.doctor.http.server.rest.HttpMethod;
import vest.doctor.http.server.rest.HttpParameterWriter;
import vest.doctor.http.server.rest.NamedHandler;
import vest.doctor.http.server.rest.Param;
import vest.doctor.http.server.rest.RouteOrchestration;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.reactive.Rx;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class OrchestrationWriter implements ProviderDefinitionListener {
    public static final String BODY_REF_NAME = "body";
    public static final String REQUEST_CONTEXT_REF = "requestContext";
    private static final String[] EMPTY_ARR = new String[0];
    private final Set<ExecutableElement> processedMethods = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Endpoint.class) == null) {
            return;
        }
        String className = providerDefinition.providedType() + "$Routing" + context.nextId();
        ClassBuilder routes = new ClassBuilder()
                .setClassName(className)
                .addImportClass(Map.class)
                .addImportClass(List.class)
                .addImportClass(Optional.class)
                .addImportClass(AnnotationMetadata.class)
                .addImportClass("vest.doctor.runtime.AnnotationMetadataImpl")
                .addImportClass("vest.doctor.runtime.AnnotationDataImpl")
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
                .addImportClass(NamedHandler.class)
                .addImportClass(Rx.class)
                .addImportClass(Flow.class)
                .addImportClass(Flow.Publisher.class)
                .addImplementsInterface(RouteOrchestration.class)
                .addField("private ProviderRegistry providerRegistry")
                .addField("private Provider<" + providerDefinition.providedType() + "> provider")
                .addField("private BodyInterchange bodyInterchange");

        MethodBuilder addRoutes = routes.newMethod("@Override public void addRoutes(ProviderRegistry providerRegistry, Router router)");
        addRoutes.line("this.providerRegistry = providerRegistry;");
        addRoutes.line("this.provider = ", ProcessorUtils.getProviderCode(providerDefinition), ";");
        addRoutes.line("this.bodyInterchange = providerRegistry.getInstance(BodyInterchange.class);");

        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Endpoint.class))
                .map(Endpoint::value)
                .orElse(new String[]{"/"});

        AtomicBoolean hasEndpointMethods = new AtomicBoolean(false);
        ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                .filter(processedMethods::add)
                .forEach(method -> {
                    List<String> httpMethods = getMethods(method);
                    if (!httpMethods.isEmpty()) {
                        String[] paths = Optional.ofNullable(method.getAnnotation(Endpoint.class))
                                .map(Endpoint::value)
                                .orElse(EMPTY_ARR);
                        String handlerName = method.getEnclosingElement().getSimpleName() + "#" + method.getSimpleName();
                        String methodName = "handle" + method.getSimpleName() + "_" + context.nextId();
                        MethodBuilder handler = routes.newMethod("public Flow.Publisher<Response> " + methodName + "(RequestContext " + REQUEST_CONTEXT_REF + ")");
                        buildRouteMethod(context, routes, handler, method);
                        for (String httpMethod : httpMethods) {
                            for (String path : paths(roots, paths)) {
                                hasEndpointMethods.set(true);
                                addRoutes.line("router.route(",
                                        ProcessorUtils.escapeAndQuoteStringForCode(httpMethod), ',',
                                        ProcessorUtils.escapeAndQuoteStringForCode(path), ',',
                                        "new NamedHandler(this::" + methodName, "," + ProcessorUtils.escapeAndQuoteStringForCode(handlerName) + "));");
                            }
                        }
                    } else if (method.getAnnotation(Endpoint.class) != null) {
                        throw new CodeProcessingException("http method is required for endpoints, e.g. @GET", method);
                    }
                });
        if (hasEndpointMethods.get()) {
            routes.writeClass(context.filer());
            context.addServiceImplementation(RouteOrchestration.class, routes.getFullyQualifiedClassName());
        }
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

    private void buildRouteMethod(AnnotationProcessorContext context,
                                  ClassBuilder classBuilder,
                                  MethodBuilder handler,
                                  ExecutableElement method) {
        checkMethodParams(context, method);
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            throw new CodeProcessingException("the return type for endpoint methods may not be void", method);
        }

        handler.line("try{");

        if (method.getReturnType().toString().equalsIgnoreCase(Object.class.getCanonicalName())) {
            throw new CodeProcessingException("endpoint methods must declare specific return types (not Object)", method);
        }

        GenericInfo returnTypeInfo = new GenericInfo(method, method.getReturnType());
        String returnTypeInfoRef = "typeInfo_" + method.getSimpleName() + context.nextId();
        classBuilder.addField("private static final TypeInfo " + returnTypeInfoRef + " = ", returnTypeInfo.newTypeInfo(context));

        boolean bodyIsPublisher = false;
        VariableElement bodyElement = bodyParameter(method);
        TypeMirror bodyParam = bodyElement != null ? bodyElement.asType() : null;
        String bodyType;
        if (bodyParam != null) {
            GenericInfo genericInfo = new GenericInfo(bodyParam);
            bodyIsPublisher = ProcessorUtils.isCompatibleWith(context, genericInfo.type(), Flow.Publisher.class);
            bodyType = "bodyType_" + method.getSimpleName() + context.nextId();
            classBuilder.addField("private static final TypeInfo ", bodyType, "=", new GenericInfo(bodyElement).newTypeInfo(context));
        } else {
            bodyType = "null";
        }
        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, classBuilder, p, p))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "provider.get()." + method.getSimpleName() + parameters;

        if (bodyIsPublisher) {
            handler.line(bodyParam, " ", BODY_REF_NAME, "=bodyInterchange.read(requestContext,", bodyType, ");");
            String resultRef = "result";
            handler.line(method.getReturnType().toString(), " result = ", callMethod, ';');
            handler.line("return bodyInterchange.write(", REQUEST_CONTEXT_REF, ", " + returnTypeInfoRef + ", " + resultRef + ");");
        } else {
            String bodyTypeString = bodyParam != null ? bodyParam.toString() : "Object";
            handler.line("return Rx.<", bodyTypeString, ">from(bodyInterchange.read(", REQUEST_CONTEXT_REF, ", ", bodyType, "))");
            handler.line(".onNext((" + BODY_REF_NAME + ", subscription, subscriber) -> {");
            handler.line("try{");
            handler.line("subscriber.onNext(" + callMethod + ");");
            handler.line("}catch(Throwable t){");
            handler.line("subscriber.onError(t);");
            handler.line("}");
            handler.line("})");
            handler.line(".mapPublisher(result -> bodyInterchange.write(requestContext, " + returnTypeInfoRef + ", result));");
        }

        handler.line("}catch(Throwable t){");
        handler.line("return Rx.error(t);");
        handler.line("}");
    }

    private void checkMethodParams(AnnotationProcessorContext context, ExecutableElement method) {
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
