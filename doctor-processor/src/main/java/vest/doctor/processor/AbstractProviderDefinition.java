package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.DoctorProvider;
import vest.doctor.ExplicitProvidedTypes;
import vest.doctor.Modules;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDependency;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.AnnotationClassValueVisitor;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class AbstractProviderDefinition implements ProviderDefinition {

    private static final Collector<CharSequence, ?, String> AS_LIST = Collectors.joining(", ", "Collections.unmodifiableList(java.util.Arrays.asList(", "))");

    protected final AnnotationProcessorContext context;
    protected final TypeElement providedType;
    protected final Element annotationSource;
    protected final List<TypeElement> hierarchy;
    protected final AnnotationMirror scope;
    protected final String qualifier;
    protected final List<String> modules;
    protected final String uniqueName;

    public AbstractProviderDefinition(AnnotationProcessorContext context, TypeElement providedType, Element annotationSource) {
        this.context = context;
        this.annotationSource = annotationSource;

        this.providedType = providedType;
        ExplicitProvidedTypes explicitProvidedTypes = annotationSource.getAnnotation(ExplicitProvidedTypes.class);
        if (explicitProvidedTypes != null) {
            List<TypeElement> types = explicitTypes(context, annotationSource);
            if (types.isEmpty()) {
                throw new IllegalArgumentException("explicitly defined types must not be empty: " + ProcessorUtils.debugString(annotationSource));
            }
            this.hierarchy = types;
        } else {
            this.hierarchy = ProcessorUtils.hierarchy(context, providedType);
        }

        this.scope = ProcessorUtils.getScope(context, annotationSource);
        this.qualifier = ProcessorUtils.getQualifier(context, annotationSource);

        List<String> modules = new LinkedList<>();
        Optional.ofNullable(annotationSource().getAnnotation(Modules.class))
                .map(Modules::value)
                .map(Arrays::asList)
                .ifPresent(modules::addAll);
        if (annotationSource().getKind() == ElementKind.METHOD) {
            Optional.ofNullable(annotationSource().getEnclosingElement())
                    .map(e -> e.getAnnotation(Modules.class))
                    .map(Modules::value)
                    .map(Arrays::asList)
                    .ifPresent(modules::addAll);
        }
        this.modules = Collections.unmodifiableList(modules);
        this.uniqueName = "inst" + context.nextId();
    }

    @Override
    public TypeElement providedType() {
        return providedType;
    }

    @Override
    public List<TypeElement> getAllProvidedTypes() {
        return hierarchy;
    }

    @Override
    public Element annotationSource() {
        return annotationSource;
    }

    @Override
    public AnnotationMirror scope() {
        return scope;
    }

    @Override
    public String qualifier() {
        return qualifier;
    }

    @Override
    public AnnotationProcessorContext context() {
        return context;
    }

    @Override
    public List<String> modules() {
        return modules;
    }

    @Override
    public List<TypeElement> hierarchy() {
        return hierarchy;
    }

    @Override
    public ProviderDependency asDependency() {
        return new Dependency(providedType, qualifier);
    }

    @Override
    public ClassBuilder getClassBuilder() {
        // handles everything but the .get()
        ClassBuilder classBuilder = new ClassBuilder();
        classBuilder.setClassName(generatedClassName())
                .addImportClass(Annotation.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(List.class)
                .addImportClass(Collections.class)
                .addImportClass(providedType().getQualifiedName().toString())
                .addImportClass(DoctorProvider.class)
                .addImplementsInterface(DoctorProvider.class.getSimpleName() + "<" + providedType().getSimpleName() + ">")
                .addField("private final ", ProviderRegistry.class.getSimpleName(), " {{providerRegistry}}");

        MethodBuilder constructor = classBuilder.newMethod("public ", generatedClassName().substring(generatedClassName().lastIndexOf('.') + 1), "(", ProviderRegistry.class, " {{providerRegistry}})");
        constructor.line("this.{{providerRegistry}} = {{providerRegistry}};");

        MethodBuilder type = classBuilder.newMethod("public Class<", providedType().getSimpleName(), "> type()");
        type.line("return " + providedType().getSimpleName() + ".class;");

        MethodBuilder qualifier = classBuilder.newMethod("public String qualifier()");
        qualifier.line("return ", Optional.ofNullable(qualifier()).map(q -> "{{providerRegistry}}.resolvePlaceholders(" + q + ")").orElse(null) + ";");

        MethodBuilder scope = classBuilder.newMethod("public Class<? extends Annotation> scope()");
        String scopeString = Optional.ofNullable(scope())
                .map(AnnotationMirror::getAnnotationType)
                .map(c -> c.asElement().toString() + ".class")
                .orElse("null");
        scope.line("return ", scopeString, ";");

        List<TypeElement> allProvidedTypes = getAllProvidedTypes();
        if (!allProvidedTypes.isEmpty()) {
            classBuilder.addField("private final List<Class<?>> allTypes = " + allProvidedTypes.stream()
                    .map(TypeElement::getQualifiedName)
                    .map(n -> n + ".class")
                    .collect(AS_LIST))
                    .addMethod("public List<Class<?>> allProvidedTypes()", b -> b.line("return allTypes;"));
        } else {
            context().errorMessage("all providers must provide at least one type: " + this);
        }

        List<? extends AnnotationMirror> annotationMirrors = annotationSource().getAnnotationMirrors();
        if (!annotationMirrors.isEmpty()) {
            classBuilder.addField("private final List<Class<? extends Annotation>> allAnnotations = " + annotationMirrors.stream()
                    .map(AnnotationMirror::getAnnotationType)
                    .map(DeclaredType::toString)
                    .map(n -> n + ".class")
                    .collect(AS_LIST))
                    .addMethod("public List<Class<? extends Annotation>> allAnnotationTypes()", b -> b.line("return allAnnotations;"));
        }

        List<String> modules = modules();
        if (!modules.isEmpty()) {
            classBuilder.addField("private final List<String> modules = " + modules.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .map(m -> '"' + m + '"')
                    .collect(AS_LIST))
                    .addMethod("public List<String> modules()", b -> b.line("return modules;"));
        }

        // must define the .get() method
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }

    private static List<TypeElement> explicitTypes(AnnotationProcessorContext context, Element annotationSource) {
        return annotationSource.getAnnotationMirrors()
                .stream()
                .filter(am -> am.getAnnotationType().toString().equals(ExplicitProvidedTypes.class.getCanonicalName()))
                .flatMap(am -> am.getElementValues().entrySet().stream())
                .filter(e -> e.getKey().getSimpleName().toString().equals(Constants.ANNOTATION_VALUE))
                .map(Map.Entry::getValue)
                .map(AnnotationClassValueVisitor::getValues)
                .flatMap(Collection::stream)
                .map(context.processingEnvironment().getElementUtils()::getTypeElement)
                .collect(Collectors.toList());
    }
}
