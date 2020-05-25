package vest.doctor.netty;

import doctor.processor.Constants;
import doctor.processor.GenericInfo;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;
import vest.doctor.StringConversionGenerator;
import vest.doctor.TypeInfo;
import vest.doctor.codegen.ClassBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
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
import java.util.stream.Collectors;

public class EndpointWriter implements ProviderDefinitionListener {

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
                    List<String> methods = getHttpMethods(method);
                    if (!methods.isEmpty()) {
                        String[] paths = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(new String[]{""});
                        List<String> fullPaths = paths(roots, paths);

                        for (String httMethod : methods) {
                            for (String fullPath : fullPaths) {
                                writeEndpoint(context, httMethod, fullPath, providerDefinition, method);
                            }
                        }
                    }
                });
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

    private static void writeEndpoint(AnnotationProcessorContext context,
                                      String httpMethod, String path,
                                      ProviderDefinition serviceProvider, ExecutableElement method) {
        String className = "Endpoint__" + context.nextId();
        String qualifiedClassName = context.generatedPackage() + "." + className;

        ClassBuilder endpoint = new ClassBuilder()
                .setClassName(qualifiedClassName)
                .setExtendsClass(Endpoint.class)
                .addImportClass(Request.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(BodyInterchange.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(Inject.class)
                .addImportClass(Singleton.class)
                .addImportClass(Named.class)
                .addImportClass(ExplicitProvidedTypes.class)
                .addImportClass(Provider.class)
                .addClassAnnotation("@Singleton")
                .addClassAnnotation("@Named(\"" + ProcessorUtils.escapeStringForCode(serviceProvider.providedType().getSimpleName() + ":" + method.getSimpleName()) + "\")")
                .addClassAnnotation("@ExplicitProvidedTypes({Endpoint.class})")
                .addField("private final Provider<" + serviceProvider.providedType() + "> provider");

        endpoint.setConstructor("@Inject public " + className + "(ProviderRegistry " + Constants.PROVIDER_REGISTRY + ")", b -> {
            String typeInfo = buildTypeInfoCode(method);
            b.line("super({}, \"{}\", \"{}\", {});", Constants.PROVIDER_REGISTRY, httpMethod, ProcessorUtils.escapeStringForCode(path), typeInfo);
            b.line("this.provider = " + Constants.PROVIDER_REGISTRY + ".getProvider(" + serviceProvider.providedType() + ".class, " + serviceProvider.qualifier() + ");");
        });

        endpoint.addMethod("protected Object executeMethod(Request request) throws Exception", b -> {
            b.line("{} instance = provider.get();", serviceProvider.providedType());

            String parameters = method.getParameters().stream()
                    .map(p -> parameterWriting(context, p, "request"))
                    .collect(Collectors.joining(", ", "(", ")"));

            boolean isVoid = method.getReturnType().getKind() == TypeKind.VOID;
            if (isVoid) {
                b.line("instance.{}{};", method.getSimpleName(), parameters);
                b.line("return null;");
            } else {
                b.line("return instance.{}{};", method.getSimpleName(), parameters);
            }
        });

        endpoint.writeClass(context.filer());
    }


    private static final List<Class<? extends Annotation>> SUPPORTED_PARAMS = Arrays.asList(Body.class, Attribute.class, PathParam.class, QueryParam.class, HeaderParam.class, CookieParam.class, BeanParam.class);
    private static final List<Class<?>> SUPPORTED_CLASSES = Arrays.asList(Request.class, URI.class);

    public static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        return parameterWriting(context, parameter, parameter, contextRef);
    }

    private static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, Element annotationSource, String contextRef) {
        if (parameter.asType().toString().equals(Request.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".uri()";
        } else if (annotationSource.getAnnotation(Body.class) != null) {
            return "(" + parameter.asType() + ") readBody(" + contextRef + ")";
        } else if (annotationSource.getAnnotation(Attribute.class) != null) {
            String name = annotationSource.getAnnotation(Attribute.class).value();
            return "(" + parameter.asType() + ") " + contextRef + ".attribute(\"" + name + "\")";
        } else if (annotationSource.getAnnotation(BeanParam.class) != null) {
            // TODO: should this pass annotationSource?
            return beanParameterCode(context, parameter, contextRef);
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
