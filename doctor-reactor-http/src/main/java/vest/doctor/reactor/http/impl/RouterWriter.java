package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.cookie.Cookie;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.processing.StringConversionGenerator;
import vest.doctor.reactor.http.Handler;
import vest.doctor.reactor.http.HttpMethod;
import vest.doctor.reactor.http.HttpRequest;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.Param;
import vest.doctor.reactor.http.Path;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.RunOn;

import javax.lang.model.element.AnnotationMirror;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouterWriter implements ProviderDefinitionListener {
    private final Set<ExecutableElement> processedMethods = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) == null) {
            return;
        }

        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                .map(Path::value)
                .orElse(new String[]{"/"});

        ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                .filter(processedMethods::add)
                .forEach(method -> {
                    List<String> httpMethods = getMethods(method);
                    if (!httpMethods.isEmpty()) {
                        String[] paths = Optional.ofNullable(method.getAnnotation(Path.class))
                                .map(Path::value)
                                .orElse(new String[]{});
                        addRoute(context, providerDefinition, httpMethods, paths(roots, paths), method);
                    } else if (method.getAnnotation(Path.class) != null) {
                        throw new CodeProcessingException("http method is required for endpoints, e.g. @GET", method);
                    }
                });
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

    private void addRoute(AnnotationProcessorContext context,
                          ProviderDefinition providerDefinition,
                          List<String> httpMethods,
                          List<String> paths,
                          ExecutableElement method) {
        // check zero or one @Body parameters
        long bodyParams = method.getParameters().stream().filter(v -> v.getAnnotation(Param.Body.class) != null).count();
        if (bodyParams > 1) {
            throw new CodeProcessingException("only one parameter may be marked with @Body in an endpoint method", method);
        }

        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;

        String className = "WiredHandler_" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;
        ClassBuilder cb = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(RequestContext.class)
                .addImportClass(Publisher.class)
                .addImportClass(Flux.class)
                .addImportClass(List.class)
                .addImportClass(Optional.class)
                .addImportClass(Cookie.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(providerDefinition.providedType())
                .addImportClass(Provider.class)
                .addImportClass(Inject.class)
                .addImportClass(Singleton.class)
                .addImportClass(ExplicitProvidedTypes.class)
                .addImportClass(Handler.class)
                .addClassAnnotation("@Singleton")
                .addClassAnnotation("@ExplicitProvidedTypes({Handler.class})")
                .setExtendsClass(AbstractWiredHandler.class)
                .addField("private static final List<String> methods = List.of(", httpMethods.stream().map(ProcessorUtils::escapeStringForCode).collect(Collectors.joining("\",\"", "\"", "\"")) + ")")
                .addField("private static final List<String> paths = List.of(", paths.stream().map(ProcessorUtils::escapeStringForCode).collect(Collectors.joining("\",\"", "\"", "\"")) + ")")
                .addField("private final Provider<", providerDefinition.providedType(), "> provider");

        String executorName = getExecutorName(method);
        cb.addMethod("@Inject public " + className + "(ProviderRegistry providerRegistry)", mb -> {
            mb.line("super(providerRegistry, \"", ProcessorUtils.escapeStringForCode(executorName), "\");");
            mb.line("this.provider = ", ProcessorUtils.getProviderCode(providerDefinition), ";");
        });

        cb.addMethod("@Override public List<String> method()", mb -> mb.line("return methods;"));
        cb.addMethod("@Override public List<String> path()", mb -> mb.line("return paths;"));
        cb.addMethod("@Override public String toString()", mb -> {
            String summary = method.getEnclosingElement().asType() + "#" + method.getSimpleName() + method.getParameters().stream().map(VariableElement::asType).map(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
            mb.line("return \"", ProcessorUtils.escapeStringForCode(summary), "#\"+hashCode();");
        });

        if (method.getReturnType().toString().equalsIgnoreCase(Object.class.getCanonicalName())) {
            throw new CodeProcessingException("endpoint methods must declare specific return types (not Object)", method);
        }

        cb.addField("private static final TypeInfo responseType = ", ProcessorUtils.newTypeInfo(new GenericInfo(method.getReturnType())));
        cb.addMethod("@Override public TypeInfo responseType()", mb -> mb.line("return responseType;"));

        boolean bodyIsPublisher = false;
        TypeMirror bodyParam = bodyParameter(method);
        String bodyType;
        if (bodyParam != null) {
            GenericInfo genericInfo = new GenericInfo(bodyParam);
            bodyIsPublisher = ProcessorUtils.isCompatibleWith(context, genericInfo.type(), Publisher.class);
            bodyType = "bodyType_" + method.getSimpleName() + context.nextId();
            cb.addField("private static final TypeInfo ", bodyType, "=", ProcessorUtils.newTypeInfo(new GenericInfo(bodyParam)));
        } else {
            bodyType = "null";
        }


        MethodBuilder handler = cb.newMethod("@Override protected Object handle(RequestContext requestContext) throws Exception");

        if (bodyIsPublisher) {
            handler.line(bodyParam, " body = bodyInterchange.read(requestContext, ", bodyType, ");");
        } else {
            String bodyTypeString = bodyParam != null ? bodyParam.toString() : "Object";
            handler.line(bodyTypeString, " body = Flux.<", bodyTypeString, ">from(bodyInterchange.read(requestContext, ", bodyType, ")).blockLast();");
        }
        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, cb, p, "requestContext"))
                .collect(Collectors.joining(",\n", "(", ")"));
        String callMethod = "provider.get()." + method.getSimpleName() + parameters + ";";

        if (isVoid) {
            handler.line(callMethod);
            handler.line("return null;");
        } else {
            handler.line(method.getReturnType().toString(), " result = ", callMethod);
            handler.line("return result;");
        }
        cb.writeClass(context.filer());
    }


    private static final List<Class<?>> SUPPORTED_CLASSES = List.of(HttpRequest.class, HttpResponse.class, URI.class);

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, String contextRef) {
        try {
            return parameterWriting(context, epHandler, parameter, parameter, contextRef);
        } catch (Throwable t) {
            throw new CodeProcessingException("error wiring endpoint parameter", parameter, t);
        }
    }

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (parameter.asType().toString().equals(RequestContext.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(HttpRequest.class.getCanonicalName())) {
            return contextRef + ".request()";
        } else if (parameter.asType().toString().equals(HttpResponse.class.getCanonicalName())) {
            return contextRef + ".response()";
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".request().uri()";
        }

        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            return "body";
        } else if (annotationSource.getAnnotation(Param.Attribute.class) != null) {
            String name = getParamName(annotationSource, parameter, Param.Attribute.class, Param.Attribute::value);
            return contextRef + ".attribute(\"" + name + "\")";
        } else if (annotationSource.getAnnotation(Param.Bean.class) != null) {
            return beanParamBuilderCall(context, handlerBuilder, parameter, contextRef) + "(" + contextRef + ")";
        } else if (annotationSource.getAnnotation(Param.Provided.class) != null) {
            return ProcessorUtils.providerLookupCode(context, parameter, Constants.PROVIDER_REGISTRY);
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = ProcessorUtils.isCompatibleWith(context, target, Optional.class);
        if (isOptional) {
            target = GenericInfo.firstParameterizedType(target)
                    .orElseThrow(() -> new CodeProcessingException("missing type for Optional", parameter));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Optional.ofNullable(")
                .append(contextRef)
                .append(".request()");

        if (annotationSource.getAnnotation(Param.Path.class) != null) {
            String name = getParamName(annotationSource, parameter, Param.Path.class, Param.Path::value);
            sb.append(".pathParam(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(Param.Query.class) != null) {
            String name = getParamName(annotationSource, parameter, Param.Query.class, Param.Query::value);
            sb.append(".queryParam(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(Param.Header.class) != null) {
            String name = getParamName(annotationSource, parameter, Param.Header.class, Param.Header::value);
            sb.append(".header(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(Param.Cookie.class) != null) {
            String name = getParamName(annotationSource, parameter, Param.Cookie.class, Param.Cookie::value);
            sb.append(".cookie(\"").append(name).append("\")).map(Cookie::value");
        } else {
            throw new CodeProcessingException("unsupported parameter - missing supported route parameter annotation", parameter);
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

    private static <A extends Annotation> String getParamName(Element element, VariableElement parameter, Class<A> parameterType, Function<A, String> mapper) {
        return Optional.of(element.getAnnotation(parameterType))
                .map(mapper)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ProcessorUtils::escapeStringForCode)
                .orElse(parameter.getSimpleName().toString());
    }

    private String beanParamBuilderCall(AnnotationProcessorContext context, ClassBuilder epHandler, VariableElement parameter, String contextRef) {
        TypeMirror typeMirror = parameter.asType();
        String typeWithoutParams = ProcessorUtils.typeWithoutParameters(typeMirror);
        String methodName = "beanParam" + context.nextId();
        MethodBuilder bean = epHandler.newMethod("public static ", typeWithoutParams, " " + methodName + "(RequestContext ", contextRef, ")");

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
        if (e.getAnnotation(Param.Path.class) != null
                || e.getAnnotation(Param.Query.class) != null
                || e.getAnnotation(Param.Header.class) != null
                || e.getAnnotation(Param.Cookie.class) != null
                || e.getAnnotation(Param.Attribute.class) != null
                || e.getAnnotation(Param.Body.class) != null
                || e.getAnnotation(Param.Provided.class) != null
                || e.getAnnotation(Param.Bean.class) != null
        ) {
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

    public static TypeMirror bodyParameter(ExecutableElement method) {
        return method.getParameters()
                .stream()
                .filter(m -> m.getAnnotation(Param.Body.class) != null)
                .map(VariableElement::asType)
                .findFirst()
                .orElse(null);
    }

    private static String getExecutorName(ExecutableElement method) {
        return Stream.of(method, method.getEnclosingElement())
                .map(e -> e.getAnnotation(RunOn.class))
                .filter(Objects::nonNull)
                .map(RunOn::value)
                .findFirst()
                .orElse(RunOn.DEFAULT_SCHEDULER);
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
