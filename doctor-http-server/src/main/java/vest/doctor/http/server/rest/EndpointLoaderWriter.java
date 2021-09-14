package vest.doctor.http.server.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import vest.doctor.ApplicationLoader;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.impl.Router;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.processing.StringConversionGenerator;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EndpointLoaderWriter implements ProviderDefinitionListener {

    private final Set<ExecutableElement> processedMethods = new HashSet<>();
    private final AtomicBoolean hasRoutes = new AtomicBoolean(false);
    private ClassBuilder config;
    private MethodBuilder stage5;

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) == null) {
            return;
        }
        initialize(context);

        String endpointRef = "endpoint" + context.nextId();
        stage5.line("Provider<", providerDefinition.providedType(), "> ", endpointRef, " = {{providerRegistry}}.getProvider(", providerDefinition.providedType(), ".class, ", providerDefinition.qualifier(), ");");
        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                .map(Path::value)
                .orElse(new String[]{"/"});

        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), Filter.class)) {
            for (String root : roots) {
                hasRoutes.set(true);
                stage5.line("router.filter(\"", ProcessorUtils.escapeStringForCode(root), "\",", endpointRef, ".get());");
            }
        }

        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), Handler.class)) {
            List<String> methods = ProcessorUtils.methodMatchingSignature(context, providerDefinition.providedType(), "handle", Request.class)
                    .map(EndpointLoaderWriter::getHttpMethods)
                    .filter(l -> !l.isEmpty())
                    .orElse(Collections.singletonList(ANY.ANY_METHOD_NAME));
            for (String root : roots) {
                for (String method : methods) {
                    stage5.line("router.route(\"",
                            ProcessorUtils.escapeStringForCode(method), "\",\"",
                            ProcessorUtils.escapeStringForCode(root),
                            "\",", endpointRef, ".get());");
                }
            }
        } else {
            ProcessorUtils.allMethods(context, providerDefinition.providedType())
                    .stream()
                    .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                    .filter(processedMethods::add)
                    .forEach(method -> {
                        List<String> methods = getHttpMethods(method);
                        if (!methods.isEmpty()) {
                            String[] paths = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(new String[]{""});
                            List<String> fullPaths = paths(roots, paths);

                            for (String httMethod : methods) {
                                for (String fullPath : fullPaths) {
                                    addRoute(stage5, context, httMethod, fullPath, endpointRef, method);
                                    hasRoutes.set(true);
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        if (hasRoutes.get()) {
            config.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, config.getFullyQualifiedClassName());
        }
    }

    private void initialize(AnnotationProcessorContext context) {
        if (config != null) {
            return;
        }
        String className = "EndpointLoader_" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;

        this.config = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImplementsInterface(EndpointLoader.class)
                .addImportClass(CompletableFuture.class)
                .addImportClass(CompletionStage.class)
                .addImportClass(BodyInterchange.class)
                .addImportClass(EndpointLinker.class)
                .addImportClass(EndpointLoader.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Optional.class)
                .addImportClass(Request.class)
                .addImportClass(Response.class)
                .addImportClass(Router.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(Inject.class)
                .addImportClass(Singleton.class)
                .addImportClass(Named.class)
                .addImportClass(ExplicitProvidedTypes.class)
                .addImportClass(Provider.class);

        this.stage5 = config.newMethod("public void stage5(ProviderRegistry {{providerRegistry}})");
        stage5.line("if(!isRouterWired({{providerRegistry}})) { return; }");
        stage5.line("Router router = {{providerRegistry}}.getInstance(Router.class);");
        stage5.line("BodyInterchange bodyInterchange = {{providerRegistry}}.getInstance(BodyInterchange.class);");

    }

    private static List<String> getHttpMethods(ExecutableElement method) {
        return method.getAnnotationMirrors()
                .stream()
                .flatMap(methodAnnotation -> methodAnnotation.getAnnotationType().asElement().getAnnotationMirrors().stream())
                .filter(annotation -> annotation.getAnnotationType().toString().equals(HttpMethod.class.getCanonicalName()))
                .flatMap(httpMethodAnnotation -> httpMethodAnnotation.getElementValues().entrySet().stream())
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(Constants.ANNOTATION_VALUE))
                .map(entry -> entry.getValue().getValue().toString())
                .collect(Collectors.toList());
    }

    private static List<String> paths(String[] roots, String[] paths) {
        List<String> allPaths = new LinkedList<>();
        for (String root : roots) {
            for (String path : paths) {
                allPaths.add(Utils.squeeze("/" + root + "/" + path, '/'));
            }
        }
        return allPaths;
    }

    private static void addRoute(MethodBuilder initialize,
                                 AnnotationProcessorContext context,
                                 String httpMethod,
                                 String path,
                                 String endpointRef,
                                 ExecutableElement method) {


        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;
        boolean bodyFuture = isBodyFuture(context, method);
        boolean completableResponse = ProcessorUtils.isCompatibleWith(context, method.getReturnType(), CompletableFuture.class);

        /**
         * router.route("GET", "/netty/hello", new EndpointLinker<>(endpoint51,null, bodyInterchange, "") {
         *             @Override
         *             protected CompletionStage<Response> handleWithProvider(demo.app.TCNettyEndpoint endpoint, Request request) {
         *                 return readFutureBody(request, bodyType, bodyInterchange)
         *                         .thenCompose(body -> {
         *                             Object result = endpoint.basic(java.util.Optional.ofNullable(request.queryParam("q")).map(java.util.function.Function.identity()), java.util.Optional.ofNullable(request.queryParam("number")).map(java.lang.Integer::parseInt).orElse(null), java.util.Optional.ofNullable(request.queryParam("number")).map(java.lang.Integer::valueOf), (java.util.List<java.io.InputStream>) request.attribute("list"), new vest.doctor.http.server.rest.POJOHelper<>(new demo.app.NettyBeanParam<>(java.util.Optional.ofNullable(request.queryParam("number")).map(java.lang.Integer::parseInt).orElse(null), request)).with(demo.app.NettyBeanParam::setQ, java.util.Optional.ofNullable(request.queryParam("q")).map(java.util.function.Function.identity())).with(demo.app.NettyBeanParam::setNumberViaMethod, java.util.Optional.ofNullable(request.queryParam("number")).map(java.lang.Integer::parseInt).orElse(null)).get());
         *                             return convertResponse(request, result, bodyInterchange);
         *                         });
         *             }
         *         });
         */
        String summary = method.getEnclosingElement().asType() + "#" + method.getSimpleName();
        initialize.line("router.route(\"",
                ProcessorUtils.escapeStringForCode(httpMethod), '"',
                ",\"", ProcessorUtils.escapeStringForCode(path), "\", new EndpointLinker<>(",
                endpointRef, ',', buildTypeInfoCode(method), ',', "bodyInterchange", ",\"", summary, "\") {");
        initialize.line("@Override protected CompletionStage<Object> handleWithProvider(", method.getEnclosingElement().asType(), " endpoint, Request request, CompletableFuture<?> body) throws Exception {");

        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, p, "request"))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "endpoint." + method.getSimpleName() + parameters + ";";

        if (bodyFuture) {
            initialize.line("CompletableFuture<?> b = body;");
        } else {
            String chainMethod = completableResponse ? "thenCompose" : "thenApply";
            initialize.line("return body.", chainMethod, "(b -> {");
        }

        if (isVoid) {
            initialize.line(callMethod);
            initialize.line("return null;");
        } else if (completableResponse) {
            initialize.line("return (CompletionStage) ", callMethod);
        } else {
            initialize.line("return ", callMethod);
        }

        if (!bodyFuture) {
            initialize.line("});");
        }
        initialize.line("}");
        initialize.line("});");
    }

    private static boolean isBodyFuture(AnnotationProcessorContext context, ExecutableElement method) {
        for (VariableElement parameter : method.getParameters()) {
            if (parameter.getAnnotation(Body.class) != null
                    && ProcessorUtils.isCompatibleWith(context, parameter.asType(), CompletableFuture.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean returnsCompletableResponse(AnnotationProcessorContext context, ExecutableElement method) {
        if (ProcessorUtils.isCompatibleWith(context, method.getReturnType(), CompletableFuture.class)) {
            return GenericInfo.firstParameterizedType(method.getReturnType())
                    .map(tm -> ProcessorUtils.isCompatibleWith(context, tm, Response.class))
                    .orElse(false);
        }
        return false;
    }

    private static final List<Class<? extends Annotation>> SUPPORTED_PARAMS = List.of(Body.class, Attribute.class, PathParam.class, QueryParam.class, HeaderParam.class, CookieParam.class, BeanParam.class);
    private static final List<Class<?>> SUPPORTED_CLASSES = List.of(Request.class, URI.class);

    private static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        try {
            return parameterWriting(context, parameter, parameter, contextRef);
        } catch (Throwable t) {
            throw new CodeProcessingException("error wiring endpoint parameter", parameter, t);
        }
    }

    private static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, Element annotationSource, String contextRef) {
        if (parameter.asType().toString().equals(Request.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".uri()";
        } else if (annotationSource.getAnnotation(Body.class) != null) {
            return "(" + parameter.asType() + ") b";
        } else if (annotationSource.getAnnotation(Attribute.class) != null) {
            String name = annotationSource.getAnnotation(Attribute.class).value();
            return contextRef + ".attribute(\"" + name + "\")";
        } else if (annotationSource.getAnnotation(BeanParam.class) != null) {
            return beanParameterCode(context, parameter, contextRef);
        } else if (annotationSource.getAnnotation(Provided.class) != null) {
            return ProcessorUtils.providerLookupCode(context, parameter, Constants.PROVIDER_REGISTRY);
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = target.toString().startsWith(Optional.class.getCanonicalName());

        if (isOptional) {
            target = GenericInfo.firstParameterizedType(target).orElse(null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Optional.ofNullable(");

        if (annotationSource.getAnnotation(PathParam.class) != null) {
            String name = annotationSource.getAnnotation(PathParam.class).value();
            sb.append("pathParam(").append(contextRef).append(",\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(QueryParam.class) != null) {
            String name = annotationSource.getAnnotation(QueryParam.class).value();
            sb.append(contextRef).append(".queryParam(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(HeaderParam.class) != null) {
            String name = annotationSource.getAnnotation(HeaderParam.class).value();
            sb.append(contextRef).append(".headers().get(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(CookieParam.class) != null) {
            String name = annotationSource.getAnnotation(CookieParam.class).value();
            sb.append(contextRef).append(".cookie(\"").append(name).append("\").value()");
        } else {
            throw new CodeProcessingException("unsupported parameter - missing supported route parameter annotation", parameter);
        }
        sb.append(")");

        sb.append(".map(");
        try {
            sb.append(getStringConversion(context, target));
        } catch (Throwable t) {
            throw new CodeProcessingException("unable to handle endpoint parameter", parameter, t);
        }
        sb.append(")");

        if (!isOptional) {
            sb.append(".orElse(null)");
        }
        return sb.toString();
    }

    private static String beanParameterCode(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        TypeMirror typeMirror = parameter.asType();
        TypeElement beanType = context.toTypeElement(typeMirror);
        ExecutableElement constructor = injectableConstructor(beanType);

        String diamond = Optional.of(beanType.getTypeParameters())
                .filter(l -> !l.isEmpty())
                .map(l -> "<>")
                .orElse("");

        String constructorParams = constructor.getParameters().stream()
                .map(p -> parameterWriting(context, p, contextRef))
                .collect(Collectors.joining(", ", "(", ")"));

        StringBuilder sb = new StringBuilder("new " + POJOHelper.class.getCanonicalName() + "<>(new " + ProcessorUtils.typeWithoutParameters(typeMirror) + diamond + constructorParams + ")");
        for (VariableElement field : ElementFilter.fieldsIn(beanType.getEnclosedElements())) {
            if (supportedParam(field)) {
                ExecutableElement setter = findCorrespondingSetter(context, field, beanType);
                VariableElement setterParameter = setter.getParameters().get(0);
                sb.append(".with(").append(beanType).append("::").append(setter.getSimpleName()).append(", ").append(parameterWriting(context, setterParameter, field, contextRef)).append(")");
            }
        }
        for (ExecutableElement method : ElementFilter.methodsIn(beanType.getEnclosedElements())) {
            if (supportedParam(method)) {
                if (method.getParameters().size() != 1) {
                    throw new CodeProcessingException("setters in BeanParam objects must have one and only one parameter", method);
                }
                VariableElement setterParameter = method.getParameters().get(0);
                sb.append(".with(").append(beanType).append("::").append(method.getSimpleName()).append(", ").append(parameterWriting(context, setterParameter, method, contextRef)).append(")");
            }
        }
        sb.append(".get()");
        return sb.toString();
    }

    private static ExecutableElement injectableConstructor(TypeElement typeElement) {
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
        return constructor;
    }

    private static boolean supportedParam(Element e) {
        for (Class<? extends Annotation> supportedParam : SUPPORTED_PARAMS) {
            if (e.getAnnotation(supportedParam) != null) {
                return true;
            }
        }
        for (Class<?> supportedClass : SUPPORTED_CLASSES) {
            if (e.asType().toString().equals(supportedClass.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    private static String getStringConversion(AnnotationProcessorContext context, TypeMirror target) {
        for (StringConversionGenerator customization : context.customizations(StringConversionGenerator.class)) {
            String function = customization.converterFunction(context, target);
            if (function != null) {
                return function;
            }
        }
        throw new CodeProcessingException("no string conversion available for: " + target);
    }

    private static final List<String> SETTER_PREFIXES = List.of("set", "is", "has");

    private static ExecutableElement findCorrespondingSetter(AnnotationProcessorContext context, VariableElement field, TypeElement beanType) {
        return ProcessorUtils.hierarchy(context, beanType)
                .stream()
                .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
                .distinct()
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

    public static String buildTypeInfoCode(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .filter(m -> m.getAnnotation(Body.class) != null)
                .map(VariableElement::asType)
                .map(GenericInfo::new)
                .map(ProcessorUtils::newTypeInfo)
                .findFirst()
                .orElse("null");
    }
}
