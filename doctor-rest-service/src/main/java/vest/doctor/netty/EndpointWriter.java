package vest.doctor.netty;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;
import vest.doctor.StringConversionGenerator;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.netty.impl.Router;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EndpointWriter implements ProviderDefinitionListener {

    private final Set<ExecutableElement> processedMethods = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) == null) {
            return;
        }

        String className = "EndpointConfig_" + providerDefinition.providedType().getSimpleName() + "_" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;

        ClassBuilder config = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImplementsInterface(EndpointConfiguration.class)
                .addImportClass(Request.class)
                .addImportClass(Router.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(BodyInterchange.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(EndpointConfiguration.class)
                .addImportClass(Inject.class)
                .addImportClass(Singleton.class)
                .addImportClass(Named.class)
                .addImportClass(ExplicitProvidedTypes.class)
                .addImportClass(Provider.class)
                .addClassAnnotation("@Singleton")
                .addClassAnnotation("@Named(\"" + className + "\")")
                .addClassAnnotation("@ExplicitProvidedTypes({EndpointConfiguration.class})")
                .addField("private final ProviderRegistry providerRegistry")
                .addField("private final BodyInterchange bodyInterchange")
                .addField("private final Router router")
                .addField("private final Provider<" + providerDefinition.providedType() + "> endpoint");

        config.addMethod("@Inject public " + className + "(ProviderRegistry {{providerRegistry}})", b -> {
            b.line("this.{{providerRegistry}} = {{providerRegistry}};");
            b.line("this.endpoint = {{providerRegistry}}.getProvider(", providerDefinition.providedType(), ".class, ", providerDefinition.qualifier(), ");");
            b.line("this.router = {{providerRegistry}}.getInstance(Router.class);");
            b.line("this.bodyInterchange = {{providerRegistry}}.getInstance(BodyInterchange.class);");
        });

        MethodBuilder initialize = config.newMethod("@Inject public void initialize()");

        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                .map(Path::value)
                .orElse(new String[]{"/"});

        AtomicBoolean methodAdded = new AtomicBoolean(false);
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
                                addRoute(initialize, context, httMethod, fullPath, method);
                                methodAdded.set(true);
                            }
                        }
                    }
                });
        if (methodAdded.get()) {
            config.writeClass(context.filer());
        }
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
                                 ExecutableElement method) {

        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, p, "request"))
                .collect(Collectors.joining(", ", "(", ")"));

        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;

        String callMethod = "endpoint.get()." + method.getSimpleName() + parameters + ";";

        StringBuilder sb = new StringBuilder();
        sb.append("request -> {");
        sb.append("TypeInfo typeInfo = ").append(buildTypeInfoCode(method)).append(';');
        if (isVoid) {
            sb.append(callMethod).append("\n");
            sb.append("return convertResponse(request, null, bodyInterchange);");
        } else {
            sb.append("Object result = ").append(callMethod).append("\n");
            sb.append("return convertResponse(request, result, bodyInterchange);");
        }
        sb.append("}");
        initialize.line("router.addRoute(\"", ProcessorUtils.escapeStringForCode(httpMethod),
                "\", \"", ProcessorUtils.escapeStringForCode(path)
                , "\", ", sb.toString(), ");");
    }

    private static final List<Class<? extends Annotation>> SUPPORTED_PARAMS = Arrays.asList(Body.class, Attribute.class, PathParam.class, QueryParam.class, HeaderParam.class, CookieParam.class, BeanParam.class);
    private static final List<Class<?>> SUPPORTED_CLASSES = Arrays.asList(Request.class, URI.class);

    public static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        return parameterWriting(context, parameter, parameter, contextRef);
    }

    @Inject
    private static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, Element annotationSource, String contextRef) {
        if (parameter.asType().toString().equals(Request.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".uri()";
        } else if (annotationSource.getAnnotation(Body.class) != null) {
            return "(" + parameter.asType() + ") readBody(" + contextRef + ", typeInfo, bodyInterchange)";
        } else if (annotationSource.getAnnotation(Attribute.class) != null) {
            String name = annotationSource.getAnnotation(Attribute.class).value();
            return "(" + parameter.asType() + ") " + contextRef + ".attribute(\"" + name + "\")";
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
        sb.append("java.util.Optional.ofNullable(");

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
            context.errorMessage("unsupported parameter");
            throw new UnsupportedOperationException();
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
                sb.append(".with(").append(beanType).append("::").append(setter.getSimpleName()).append(", ").append(parameterWriting(context, setterParameter, field, contextRef)).append(")");
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
            throw new IllegalArgumentException("failed to find injectable constructor for the BeanParam: " + typeElement);
        }
        return constructor;
    }

    private static boolean supportedParam(VariableElement e) {
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
        context.errorMessage("no string conversion available for: " + target);
        return null;
    }

    private static ExecutableElement findCorrespondingSetter(AnnotationProcessorContext context, VariableElement field, TypeElement beanType) {
        return ProcessorUtils.hierarchy(context, beanType)
                .stream()
                .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
                .distinct()
                .filter(method -> {
                    if (method.getSimpleName().toString().equalsIgnoreCase("set" + field.getSimpleName())
                            || method.getSimpleName().toString().equalsIgnoreCase("is" + field.getSimpleName())) {
                        if (method.getParameters().size() != 1) {
                            throw new IllegalArgumentException("setters for BeanParam fields must have one and only one parameter");
                        }
                        return true;
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing setter method for BeanParam field: " + field + " in " + field.getEnclosingElement()));
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
