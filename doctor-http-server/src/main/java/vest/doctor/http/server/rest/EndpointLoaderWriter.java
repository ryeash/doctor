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
import vest.doctor.http.server.rest.Param.Type;
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

import static vest.doctor.http.server.rest.Param.Type.Body;

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

        TypeElement endpointType = providerDefinition.providedType();
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
                    .map(m -> m.getAnnotation(Endpoint.class))
                    .map(Endpoint::method)
                    .map(List::of)
                    .orElse(Collections.singletonList(HttpMethod.ANY));
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
                    .filter(m -> m.getAnnotation(Endpoint.class) != null)
                    .forEach(method -> {
                        Endpoint endpoint = method.getAnnotation(Endpoint.class);
                        if (endpoint.method().length == 0) {
                            throw new CodeProcessingException("endpoints must have at least one method", method);
                        }
                        if (endpoint.path().length == 0) {
                            throw new CodeProcessingException("endpoints must have at least one path", method);
                        }
                        for (String fullPath : paths(roots, endpoint.path())) {
                            for (String httMethod : endpoint.method()) {
                                addRoute(stage5, context, httMethod, fullPath, endpointType, endpointRef, method);
                                hasRoutes.set(true);
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
                .addImportClass(Utils.class)
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
                                 TypeElement endpointType,
                                 String endpointRef,
                                 ExecutableElement method) {
        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;
        boolean completableResponse = ProcessorUtils.isCompatibleWith(context, method.getReturnType(), CompletableFuture.class);

        String summary = method.getEnclosingElement().asType() + "#" + method.getSimpleName() + method.getParameters().stream().map(VariableElement::asType).map(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
        initialize.line("router.route(\"",
                ProcessorUtils.escapeStringForCode(httpMethod), '"',
                ",\"", ProcessorUtils.escapeStringForCode(path), "\", new EndpointLinker<>(",
                endpointRef, ',', buildTypeInfoCode(method), ',', "bodyInterchange", ",\"", summary, "\",");
        initialize.line("(ep, req, flow) -> {");
        initialize.line("return flow.with(ep, req, (tuple, subscription, emitter) -> {");
        initialize.line(endpointType, " endpoint = tuple.first();");
        initialize.line("Request request = tuple.second();");
        initialize.line("Object b = tuple.third();");

        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, p, "request"))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "endpoint." + method.getSimpleName() + parameters + ";";

        if (isVoid) {
            initialize.line(callMethod);
            initialize.line("Object result = null;");
        } else {
            initialize.line("Object result = ", callMethod);
        }
        initialize.line("emitter.emit(result);");

//        if (completableResponse) {
//            initialize.line(".mapFuture(java.util.function.Function.identity())");
//        }

        initialize.line("});");
        initialize.line("}));");
    }

    private static boolean isBodyFuture(AnnotationProcessorContext context, ExecutableElement method) {
        for (VariableElement parameter : method.getParameters()) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null
                    && param.type() == Body
                    && ProcessorUtils.isCompatibleWith(context, parameter.asType(), CompletableFuture.class)) {
                return true;
            }
        }
        return false;
    }

    private static final List<Class<?>> SUPPORTED_CLASSES = List.of(Request.class, Response.class, URI.class);

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
        } else if (parameter.asType().toString().equals(Response.class.getCanonicalName())) {
            return contextRef + ".createResponse()";
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".uri()";
        }


        Param param = annotationSource.getAnnotation(Param.class);
        if (param == null) {
            throw new CodeProcessingException("unsupported parameter - missing @Param annotation", parameter);
        }
        Type type = param.type();
        String name = Optional.ofNullable(param.name())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(parameter.getSimpleName().toString());
        switch (type) {
            case Body:
                return "(" + parameter.asType() + ") b";
            case Attribute:
                return contextRef + ".attribute(\"" + name + "\")";
            case Bean:
                return beanParameterCode(context, parameter, contextRef);
            case Provided:
                return ProcessorUtils.providerLookupCode(context, parameter, Constants.PROVIDER_REGISTRY);
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = ProcessorUtils.isCompatibleWith(context, target, Optional.class);
        if (isOptional) {
            target = GenericInfo.firstParameterizedType(target)
                    .orElseThrow(() -> new CodeProcessingException("missing type for Optional", parameter));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Optional.ofNullable(");

        switch (type) {
            case Path -> sb.append("Utils.pathParam(").append(contextRef).append(",\"").append(name).append("\")");
            case Query -> sb.append(contextRef).append(".queryParam(\"").append(name).append("\")");
            case Header -> sb.append(contextRef).append(".headers().get(\"").append(name).append("\")");
            case Cookie -> sb.append(contextRef).append(".cookie(\"").append(name).append("\").value()");
            default -> throw new CodeProcessingException("unsupported parameter - missing supported route parameter annotation", parameter);
        }
        sb.append(")");
        sb.append(".map(");
        sb.append(getStringConversion(context, target));
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
                sb.append("\n.with(").append(beanType).append("::").append(setter.getSimpleName()).append(", ").append(parameterWriting(context, setterParameter, field, contextRef)).append(")");
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
        if (e.getAnnotation(Param.class) != null) {
            return true;
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
                .filter(m -> getParamType(m) == Body)
                .map(VariableElement::asType)
                .map(GenericInfo::new)
                .map(ProcessorUtils::newTypeInfo)
                .findFirst()
                .orElse("null");
    }

    private static Type getParamType(Element element) {
        return Optional.ofNullable(element.getAnnotation(Param.class))
                .map(Param::type)
                .orElse(null);
    }
}
