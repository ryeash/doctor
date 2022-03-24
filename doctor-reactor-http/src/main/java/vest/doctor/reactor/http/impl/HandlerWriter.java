package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.cookie.Cookie;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import vest.doctor.AnnotationMetadata;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.reactor.http.Endpoint;
import vest.doctor.reactor.http.Handler;
import vest.doctor.reactor.http.HttpMethod;
import vest.doctor.reactor.http.HttpParameterWriter;
import vest.doctor.reactor.http.Param;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.RunOn;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HandlerWriter implements ProviderDefinitionListener {
    public static final String BODY_REF_NAME = "body";
    private final Set<ExecutableElement> processedMethods = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Endpoint.class) == null) {
            return;
        }

        String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Endpoint.class))
                .map(Endpoint::value)
                .orElse(new String[]{"/"});

        ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                .filter(processedMethods::add)
                .forEach(method -> {
                    List<String> httpMethods = getMethods(method);
                    if (!httpMethods.isEmpty()) {
                        String[] paths = Optional.ofNullable(method.getAnnotation(Endpoint.class))
                                .map(Endpoint::value)
                                .orElse(new String[]{});
                        addRoute(context, providerDefinition, httpMethods, paths(roots, paths), method);
                    } else if (method.getAnnotation(Endpoint.class) != null) {
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
        checkMethodParams(context, method);
        boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;

        String packageName = context.generatedPackageName(method.getEnclosingElement());
        providerDefinition.providedType();

        String className = "WiredHandler_" + context.nextId();
        String qualifiedClassName = packageName + "." + className;
        ClassBuilder cb = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(RequestContext.class)
                .addImportClass(Publisher.class)
                .addImportClass(AnnotationMetadata.class)
                .addImportClass(Map.class)
                .addImportClass("vest.doctor.runtime.AnnotationMetadataImpl")
                .addImportClass("vest.doctor.runtime.AnnotationDataImpl")
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
                .addField("private static final List<String> methods = List.of(", httpMethods.stream().map(ProcessorUtils::escapeAndQuoteStringForCode).collect(Collectors.joining(",")) + ")")
                .addField("private static final List<String> paths = List.of(", paths.stream().map(ProcessorUtils::escapeAndQuoteStringForCode).collect(Collectors.joining(",")) + ")")
                .addField("private final Provider<", providerDefinition.providedType(), "> provider");

        String executorName = getExecutorName(method);
        cb.addMethod("@Inject public " + className + "(ProviderRegistry providerRegistry)", mb -> {
            mb.line("super(providerRegistry,", ProcessorUtils.escapeAndQuoteStringForCode(executorName), ");");
            mb.line("this.provider = ", ProcessorUtils.getProviderCode(providerDefinition), ";");
        });

        cb.addMethod("@Override public List<String> method()", mb -> mb.line("return methods;"));
        cb.addMethod("@Override public List<String> path()", mb -> mb.line("return paths;"));
        cb.addMethod("@Override public String toString()", mb -> {
            String summary = method.getEnclosingElement().asType() + "#" + method.getSimpleName() + method.getParameters().stream().map(VariableElement::asType).map(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
            mb.line("return \"", ProcessorUtils.escapeStringForCode(summary), "\";");
        });

        if (method.getReturnType().toString().equalsIgnoreCase(Object.class.getCanonicalName())) {
            throw new CodeProcessingException("endpoint methods must declare specific return types (not Object)", method);
        }

        cb.addField("private static final TypeInfo responseType = ", new GenericInfo(method, method.getReturnType()).newTypeInfo(context));
        cb.addMethod("@Override public TypeInfo responseType()", mb -> mb.line("return responseType;"));

        boolean bodyIsPublisher = false;
        VariableElement bodyElement = bodyParameter(method);
        TypeMirror bodyParam = bodyElement != null ? bodyElement.asType() : null;
        String bodyType;
        if (bodyParam != null) {
            GenericInfo genericInfo = new GenericInfo(bodyParam);
            bodyIsPublisher = ProcessorUtils.isCompatibleWith(context, genericInfo.type(), Publisher.class);
            bodyType = "bodyType_" + method.getSimpleName() + context.nextId();
            cb.addField("private static final TypeInfo ", bodyType, "=", new GenericInfo(bodyElement).newTypeInfo(context));
        } else {
            bodyType = "null";
        }


        MethodBuilder handler = cb.newMethod("@Override protected Object handle(RequestContext requestContext) throws Exception");

        if (bodyIsPublisher) {
            handler.line(bodyParam, " ", BODY_REF_NAME, "=bodyInterchange.read(requestContext,", bodyType, ");");
        } else {
            String bodyTypeString = bodyParam != null ? bodyParam.toString() : "Object";
            handler.line(bodyTypeString, " ", BODY_REF_NAME, "=Flux.<", bodyTypeString, ">from(bodyInterchange.read(requestContext, ", bodyType, ")).blockLast();");
        }
        String parameters = method.getParameters().stream()
                .map(p -> parameterWriting(context, cb, p, p, "requestContext"))
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

    private String parameterWriting(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        for (HttpParameterWriter customization : context.customizations(HttpParameterWriter.class)) {
            String code = customization.writeParameter(context, handlerBuilder, parameter, annotationSource, contextRef);
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
