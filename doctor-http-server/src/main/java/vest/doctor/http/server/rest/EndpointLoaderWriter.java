package vest.doctor.http.server.rest;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.flow.Flo;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private ClassBuilder loader;
    private MethodBuilder stage5;

    private final Map<String, String> beanParamTypeToMethod = new HashMap<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) == null) {
            return;
        }
        initialize(context);

        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                .map(Path::value)
                .orElse(new String[]{"/"});

        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), Filter.class)) {
            for (String root : roots) {
                hasRoutes.set(true);
                stage5.line("router.filter(\"", ProcessorUtils.escapeStringForCode(root), "\",", "{{providerRegistry}}.getInstance(", providerDefinition.providedType(), ".class, ", providerDefinition.qualifier(), "));");
            }
        }

        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), Handler.class)) {
            String endpointRef = "endpoint" + context.nextId();
            stage5.line("Provider<", providerDefinition.providedType(), "> ", endpointRef, " = {{providerRegistry}}.getProvider(", providerDefinition.providedType(), ".class, ", providerDefinition.qualifier(), ");");
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
            List<ExecutableElement> endpointMethods = ProcessorUtils.allMethods(context, providerDefinition.providedType())
                    .stream()
                    .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                    .filter(processedMethods::add)
                    .filter(m -> m.getAnnotation(Endpoint.class) != null)
                    .toList();
            if (!endpointMethods.isEmpty()) {
                ClassBuilder classBuilder = initEndpointManagerClass(context, providerDefinition);
                String instanceName = "handlerInst" + context.nextId();
                stage5.line(classBuilder.getFullyQualifiedClassName(), " ", instanceName, " = new ", classBuilder.getFullyQualifiedClassName(), "({{providerRegistry}});");
                endpointMethods.forEach(method -> {
                    Endpoint endpoint = method.getAnnotation(Endpoint.class);
                    if (endpoint.method().length == 0) {
                        throw new CodeProcessingException("endpoints must have at least one method", method);
                    }
                    if (endpoint.path().length == 0) {
                        throw new CodeProcessingException("endpoints must have at least one path", method);
                    }
                    addRoute(context, classBuilder, instanceName, endpoint.method(), paths(roots, endpoint.path()), method);
                });
                classBuilder.writeClass(context.filer());
            }
        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        if (hasRoutes.get()) {
            loader.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, loader.getFullyQualifiedClassName());
            loader = null;
            hasRoutes.set(false);
            processedMethods.clear();
        }
    }

    private void initialize(AnnotationProcessorContext context) {
        if (loader != null) {
            return;
        }
        String className = "EndpointLoader_" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;

        this.loader = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImplementsInterface(ApplicationLoader.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Router.class)
                .addImportClass(Handler.class)
                .addImportClass(Handler.Holder.class)
                .addImportClass(Provider.class)
                .addClassAnnotation("@SuppressWarnings(\"unchecked\")");

        this.stage5 = loader.newMethod("public void stage5(ProviderRegistry {{providerRegistry}})");
        stage5.line("Router router = {{providerRegistry}}.getProviderOpt(Router.class).map(Provider::get).orElse(null);");
        stage5.line("if(router == null) { return; }");

        loader.addMethod("@Override public int priority()", m -> m.line("return Integer.MAX_VALUE;"));
    }

    private ClassBuilder initEndpointManagerClass(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        String className = "EndpointManager_" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;

        ClassBuilder builder = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImportClass(CompletableFuture.class)
                .addImportClass(CompletionStage.class)
                .addImportClass(BodyInterchange.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Optional.class)
                .addImportClass(Request.class)
                .addImportClass(Response.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(Utils.class)
                .addImportClass(Flo.class)
                .addImportClass(Provider.class)
                .addClassAnnotation("@SuppressWarnings(\"unchecked\")");

        builder.addField("private final ProviderRegistry {{providerRegistry}}");
        builder.addField("private final BodyInterchange bodyInterchange");
        MethodBuilder constructor = builder.newMethod("public ", className, "(ProviderRegistry {{providerRegistry}})");
        constructor.line("this.{{providerRegistry}} = {{providerRegistry}};");
        constructor.line("this.bodyInterchange = {{providerRegistry}}.getInstance(BodyInterchange.class);");

        String endpointRef = "endpoint" + context.nextId();
        builder.addField("private final Provider<", providerDefinition.providedType(), "> ", endpointRef);
        constructor.line("this.", endpointRef, " = {{providerRegistry}}.getProvider(", providerDefinition.providedType(), ".class, ", providerDefinition.qualifier(), ");");

        builder.addMethod("private " + providerDefinition.providedType() + " __endpoint()", m -> m.line("return ", endpointRef, ".get();"));
        return builder;
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

    private void addRoute(AnnotationProcessorContext context,
                          ClassBuilder epHandler,
                          String epHandlerInstance,
                          String[] httpMethods,
                          List<String> paths,
                          ExecutableElement method) {
        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;

        String summary = method.getEnclosingElement().asType() + "#" + method.getSimpleName() + method.getParameters().stream().map(VariableElement::asType).map(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
        String methodName = "handler_" + method.getSimpleName() + context.nextId();

        String bodyType;
        if (hasBodyTypeInfo(method)) {
            bodyType = "bodyType_" + method.getSimpleName() + context.nextId();
            epHandler.addField("private static final TypeInfo ", bodyType, "=", buildTypeInfoCode(method));
        } else {
            bodyType = "null";
        }

        MethodBuilder handler = epHandler.newMethod("public Flo<?, Response> ", methodName, "(Request request) throws Exception");
        handler.line("return bodyInterchange.read(request, ", bodyType, ")");
        handler.line(".step((b, subscription, emitter) -> {");

        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, epHandler, p, "request"))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "__endpoint()." + method.getSimpleName() + parameters + ";";

        if (isVoid) {
            handler.line(callMethod);
            handler.line("emitter.emit(null);");
        } else {
            handler.line(method.getReturnType().toString(), " result = ", callMethod);
            handler.line("emitter.emit(result);");
        }
        handler.line("})");
        handler.line(".chain(response -> bodyInterchange.write(request, response));");

        for (String fullPath : paths) {
            for (String httpMethod : httpMethods) {
                stage5.line("router.route(\"",
                        ProcessorUtils.escapeStringForCode(httpMethod), '"',
                        ",\"", ProcessorUtils.escapeStringForCode(fullPath),
                        "\", new Holder(", epHandlerInstance,
                        "::", methodName, ", \"", ProcessorUtils.escapeStringForCode(summary), "\"));");
            }
        }
    }

    private static final List<Class<?>> SUPPORTED_CLASSES = List.of(Request.class, Response.class, URI.class);

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, String contextRef) {
        try {
            return parameterWriting(context, epHandler, parameter, parameter, contextRef);
        } catch (Throwable t) {
            throw new CodeProcessingException("error wiring endpoint parameter", parameter, t);
        }
    }

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, Element annotationSource, String contextRef) {
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
                return beanParamBuilderCall(context, epHandler, parameter, contextRef) + "(" + contextRef + ")";
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

    private String beanParamBuilderCall(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, String contextRef) {
        TypeMirror typeMirror = parameter.asType();
        String typeWithoutParams = ProcessorUtils.typeWithoutParameters(typeMirror);
        if (beanParamTypeToMethod.containsKey(typeWithoutParams)) {
            return beanParamTypeToMethod.get(typeWithoutParams);
        }
        String methodName = "beanParam" + context.nextId();
        beanParamTypeToMethod.put(typeWithoutParams, methodName);
        MethodBuilder bean = epHandler.newMethod("public static ", typeWithoutParams, " " + methodName + "(Request ", contextRef, ")");

        TypeElement beanType = context.toTypeElement(typeMirror);
        ExecutableElement constructor = injectableConstructor(beanType);

        String diamond = Optional.of(beanType.getTypeParameters())
                .filter(l -> !l.isEmpty())
                .map(l -> "<>")
                .orElse("");

        String constructorParams = constructor.getParameters().stream()
                .map(p -> parameterWriting(context, epHandler, p, contextRef))
                .collect(Collectors.joining(", ", "(", ")"));

        bean.line(ProcessorUtils.typeWithoutParameters(typeMirror), " bean = new ", ProcessorUtils.typeWithoutParameters(typeMirror), diamond, constructorParams, ";");

        for (VariableElement field : ElementFilter.fieldsIn(beanType.getEnclosedElements())) {
            if (supportedParam(field)) {
                ExecutableElement setter = findCorrespondingSetter(context, field, beanType);
                VariableElement setterParameter = setter.getParameters().get(0);
                bean.line("bean.", setter.getSimpleName(), "(", parameterWriting(context, epHandler, setterParameter, field, contextRef), ");");
            }
        }
        for (ExecutableElement method : ElementFilter.methodsIn(beanType.getEnclosedElements())) {
            if (supportedParam(method)) {
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

    public static boolean hasBodyTypeInfo(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .anyMatch(m -> getParamType(m) == Body);
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
