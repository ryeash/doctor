package vest.doctor.processor;

import jakarta.inject.Inject;
import vest.doctor.Activation;
import vest.doctor.DoctorProvider;
import vest.doctor.Import;
import vest.doctor.Primary;
import vest.doctor.PrimaryProviderWrapper;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.AnnotationClassValueVisitor;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;
import vest.doctor.processing.ProviderCustomizationPoint;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;
import vest.doctor.processing.ProviderDefinitionProcessor;
import vest.doctor.processing.ProviderDependency;
import vest.doctor.processing.ScopeWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vest.doctor.codegen.Constants.PROVIDER_REGISTRY;

@SupportedSourceVersion(SourceVersion.RELEASE_18)
@SupportedOptions({DoctorAnnotationProcessor.PACKAGE_NAME_OPTION, DoctorAnnotationProcessor.IGNORE_PACKAGES})
public class DoctorAnnotationProcessor extends AbstractProcessor implements AnnotationProcessorContext {

    /**
     * Sets the package name for the generated classes. If unset, the default
     * uses a random package structure to avoid collisions.
     */
    public static final String PACKAGE_NAME_OPTION = "doctor.generated.packagename";

    /**
     * Defines a set of package prefixes that should be ignored during processing.
     */
    public static final String IGNORE_PACKAGES = "doctor.ignore.packages";

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private ProcessingEnvironment processingEnv;
    private String generatedPackage;
    private List<String> ignorePackages;
    private final List<TypeElement> annotationsToProcess = new LinkedList<>();

    private final List<CustomizationPoint> customizationPoints = new LinkedList<>();
    private final List<ProviderDefinition> providerDefinitions = new LinkedList<>();

    private final Set<ProviderDependency> additionalSatisfiedDependencies = new HashSet<>();

    private long start;
    private AppLoaderWriter appLoaderWriter;

    private final Map<Class<?>, Collection<String>> serviceImplementations = new HashMap<>();
    private final DependencyGraph graph = new DependencyGraph();
    private final AtomicBoolean importsScanned = new AtomicBoolean(false);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.start = System.currentTimeMillis();
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.generatedPackage = processingEnv.getOptions().getOrDefault(PACKAGE_NAME_OPTION, "vest.doctor.generated");
        this.ignorePackages = Stream.of(processingEnv.getOptions().getOrDefault(IGNORE_PACKAGES, "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        loadConf(new DefaultProcessorConfiguration());
        for (ProcessorConfiguration processorConfiguration : ServiceLoader.load(ProcessorConfiguration.class, DoctorAnnotationProcessor.class.getClassLoader())) {
            loadConf(processorConfiguration);
        }
        customizationPoints.sort(Prioritized.COMPARATOR);

        addSatisfiedDependency(ProviderRegistry.class, null);
        addSatisfiedDependency(ConfigurationFacade.class, null);
        addSatisfiedDependency(EventBus.class, null);
        appLoaderWriter = new AppLoaderWriter(this);
    }

    private void loadConf(ProcessorConfiguration processorConfiguration) {
        for (Class<? extends Annotation> supportedAnnotation : processorConfiguration.supportedAnnotations()) {
            annotationsToProcess.add(processingEnv.getElementUtils().getTypeElement(supportedAnnotation.getCanonicalName()));
        }
        customizationPoints.addAll(processorConfiguration.customizationPoints());
    }

    private final Set<Element> processedElements = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processImports(roundEnv);
        Stream.of(annotationsToProcess, annotations)
                .flatMap(Collection::stream)
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(processedElements::add)
                .forEach(this::processElement);

        customizationPoints.forEach(c -> c.finish(this));
        appLoaderWriter.finish();
        appLoaderWriter = new AppLoaderWriter(this);

        if (roundEnv.processingOver()) {
            writeServicesResource();
            compileTimeDependencyCheck();
            infoMessage("doctor processing took " + (System.currentTimeMillis() - start) + "ms");
        }
        return new HashSet<>(annotationsToProcess).containsAll(annotations);
    }

    private void processImports(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Import.class)
                .stream()
                .map(e -> e.getAnnotation(Import.class))
                .filter(Objects::nonNull)
                .flatMap(imp -> Arrays.stream(imp.value()))
                .map(processingEnv.getElementUtils()::getAllPackageElements)
                .flatMap(Set::stream)
                .map(PackageElement::getEnclosedElements)
                .flatMap(List::stream)
                .filter(processedElements::add)
                .forEach(this::processElement);
    }

    private void processElement(Element annotatedElement) {
        try {
            if (shouldIgnore(annotatedElement)) {
                infoMessage("ignoring element " + annotatedElement + " due to configured ignorePackages");
                return;
            }
            for (ProviderDefinitionProcessor providerDefinitionProcessor : customizations(ProviderDefinitionProcessor.class)) {
                ProviderDefinition provDef = providerDefinitionProcessor.process(this, annotatedElement);
                if (provDef != null) {
                    errorChecking(provDef);
                    providerDefinitions.add(provDef);
                    for (ProviderDefinitionListener providerDefinitionListener : customizations(ProviderDefinitionListener.class)) {
                        providerDefinitionListener.process(this, provDef);
                    }
                    ClassBuilder classBuilder = provDef.getClassBuilder();
                    if (classBuilder != null) {
                        classBuilder.writeClass(filer());
                    }
                    writeInProvider(provDef);
                    break;
                }
            }
        } catch (Throwable t) {
            throw new CodeProcessingException("error processing", annotatedElement, t);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return annotationsToProcess.stream()
                .map(TypeElement::asType)
                .map(TypeMirror::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public ProcessingEnvironment processingEnvironment() {
        return processingEnv;
    }

    @Override
    public String generatedPackage() {
        return generatedPackage;
    }

    @Override
    public boolean isProvided(ProviderDependency dependency) {
        if (additionalSatisfiedDependencies.contains(dependency)) {
            return true;
        }
        for (ProviderDefinition providerDefinition : providerDefinitions) {
            for (TypeElement type : providerDefinition.getAllProvidedTypes()) {
                Dependency provided = new Dependency(type, providerDefinition.qualifier());
                if (Objects.equals(dependency, provided)) {
                    return true;
                }
                if (providerDefinition.markedWith(Primary.class)) {
                    Dependency primary = new Dependency(type, null);
                    if (Objects.equals(dependency, primary)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Number nextId() {
        return idGenerator.incrementAndGet();
    }

    @Override
    public void registerDependency(ProviderDependency target, ProviderDependency dependency) {
        graph.registerDependency(target, dependency);
    }

    @Override
    public void addSatisfiedDependency(Class<?> type, String qualifier) {
        additionalSatisfiedDependencies.add(buildDependency(processingEnv.getElementUtils().getTypeElement(type.getCanonicalName()), qualifier, false));
    }

    @Override
    public ProviderDependency buildDependency(TypeElement type, String qualifier, boolean required) {
        if (qualifier == null) {
            return new Dependency(type, null, required);
        } else if (qualifier.startsWith("@") || qualifier.startsWith("\"")) {
            return new Dependency(type, qualifier, required);
        } else {
            return new Dependency(type, "\"" + qualifier + "\"", required);
        }
    }

    @Override
    public <T extends CustomizationPoint> List<T> customizations(Class<T> type) {
        return customizationPoints.stream()
                .filter(type::isInstance)
                .distinct()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    @Override
    public void addServiceImplementation(Class<?> serviceInterface, String fullyQualifiedClassName) {
        serviceImplementations.computeIfAbsent(serviceInterface, v -> new HashSet<>())
                .add(fullyQualifiedClassName);
    }

    @Override
    public MethodBuilder appLoaderStage1() {
        return appLoaderWriter.stage1();
    }

    @Override
    public MethodBuilder appLoaderStage2() {
        return appLoaderWriter.stage2();
    }

    @Override
    public MethodBuilder appLoaderStage3() {
        return appLoaderWriter.stage3();
    }

    @Override
    public MethodBuilder appLoaderStage4() {
        return appLoaderWriter.stage4();
    }

    @Override
    public MethodBuilder appLoaderStage5() {
        return appLoaderWriter.stage5();
    }

    private void errorChecking(ProviderDefinition providerDefinition) {
        for (VariableElement field : ElementFilter.fieldsIn(processingEnvironment().getElementUtils().getAllMembers(providerDefinition.providedType()))) {
            if (field.getAnnotation(Inject.class) != null) {
                throw new CodeProcessingException("field injection is not supported", field);
            }
        }
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PreDestroy", preDestroy -> {
            for (ExecutableElement method : ProcessorUtils.allMethods(this, providerDefinition.providedType())) {
                if (method.getAnnotation(preDestroy) != null) {
                    throw new CodeProcessingException("@PreDestroy is not supported (use @DestroyMethod)", method);
                }
            }
        });
    }

    private void writeInProvider(ProviderDefinition providerDefinition) {
        ClassBuilder appLoader = appLoaderWriter.classBuilder();
        appLoader.addImportClass(ProcessorUtils.typeWithoutParameters(providerDefinition.providedType().asType()));

        String creator = "new " + providerDefinition.generatedClassName() + "({{providerRegistry}})";

        for (ProviderCustomizationPoint providerCustomizationPoint : customizations(ProviderCustomizationPoint.class)) {
            creator = providerCustomizationPoint.wrap(this, providerDefinition, creator, PROVIDER_REGISTRY);
        }

        List<TypeElement> activationPredicates = allActivationRequirements(providerDefinition);
        boolean hasActivationPredicates = !activationPredicates.isEmpty();
        MethodBuilder stage = hasActivationPredicates ? appLoaderWriter.stage3() : appLoaderWriter.stage2();

        if (providerDefinition.scope() != null) {
            for (ScopeWriter scopeWriter : customizations(ScopeWriter.class)) {
                String wrapped = scopeWriter.wrapScope(this, providerDefinition, creator);
                if (wrapped != null) {
                    creator = wrapped;
                    break;
                }
            }
        }

        stage.line(DoctorProvider.class, "<", providerDefinition.providedType().getSimpleName(), "> ", providerDefinition.uniqueInstanceName(), " = ", creator, ";");
        if (hasActivationPredicates) {
            String createPredicates = activationPredicates.stream().map(c -> "new " + c + "()").collect(Collectors.joining(","));
            stage.line("if(checkActive({{providerRegistry}}, ", providerDefinition.uniqueInstanceName(), ", ", createPredicates, ")){");
        }
        stage.line("{{providerRegistry}}.register(", providerDefinition.uniqueInstanceName(), ");");
        if (providerDefinition.markedWith(Primary.class)) {
            if (providerDefinition.qualifier() == null) {
                throw new IllegalArgumentException("unqualified provider can not be marked @Primary: " + providerDefinition);
            }
            stage.line("{{providerRegistry}}.register(new ", PrimaryProviderWrapper.class, "(", providerDefinition.uniqueInstanceName(), "));");
        }
        if (hasActivationPredicates) {
            stage.line("}");
        }
    }

    private void writeServicesResource() {
        try {
            for (Map.Entry<Class<?>, Collection<String>> entry : serviceImplementations.entrySet()) {
                FileObject sourceFile = filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + entry.getKey().getCanonicalName());
                try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                    for (String implementation : entry.getValue()) {
                        out.println(implementation);
                    }
                }
            }
        } catch (IOException e) {
            throw new CodeProcessingException("error writing services resources", e);
        }
    }

    private void compileTimeDependencyCheck() {
        // missing provider check
        for (Map.Entry<ProviderDependency, Set<ProviderDependency>> entry : graph.getMap().entrySet()) {
            ProviderDependency target = entry.getKey();
            for (ProviderDependency dependency : entry.getValue()) {
                if (dependency != null && dependency.required() && !isProvided(dependency)) {
                    Stream<ProviderDependency> deps = providerDefinitions.stream().map(ProviderDefinition::asDependency);
                    Stream<ProviderDependency> add = additionalSatisfiedDependencies.stream();
                    throw new CodeProcessingException("missing provider dependency for\ntarget: " + target + "\ndependency: " + dependency + "\nknown types:\n  " +
                            Stream.of(deps, add).flatMap(Function.identity()).map(String::valueOf).collect(Collectors.joining("\n  ")));
                }
            }
        }
    }

    private boolean shouldIgnore(Element annotatedElement) {
        String fullTypeName;
        if (annotatedElement instanceof TypeElement) {
            fullTypeName = annotatedElement.asType().toString();
        } else if (annotatedElement instanceof ExecutableElement) {
            fullTypeName = annotatedElement.getEnclosingElement().asType().toString();
        } else {
            fullTypeName = "";
        }
        return ignorePackages.stream().anyMatch(fullTypeName::startsWith);
    }

    private List<TypeElement> allActivationRequirements(ProviderDefinition providerDefinition) {
        return Stream.of(providerDefinition.annotationSource().getEnclosingElement(), providerDefinition.annotationSource())
                .filter(Objects::nonNull)
                .flatMap(s -> s.getAnnotationMirrors().stream())
                .flatMap(am -> {
                    List<? extends AnnotationMirror> annotationMirrors = am.getAnnotationType().asElement().getAnnotationMirrors();
                    return Stream.concat(Stream.of(am), annotationMirrors.stream());
                })
                .filter(am -> am.getAnnotationType().toString().equals(Activation.class.getCanonicalName()))
                .flatMap(am -> am.getElementValues().entrySet().stream())
                .filter(e -> e.getKey().getSimpleName().toString().equals(Constants.ANNOTATION_VALUE))
                .map(Map.Entry::getValue)
                .map(AnnotationClassValueVisitor::getValues)
                .flatMap(Collection::stream)
                .map(className -> {
                    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(className);
                    boolean validConstructor = ElementFilter.constructorsIn(typeElement.getEnclosedElements())
                            .stream()
                            .anyMatch(con -> con.getModifiers().contains(Modifier.PUBLIC) && con.getParameters().isEmpty());
                    if (!validConstructor) {
                        throw new CodeProcessingException("no valid constructor for activation predicate for " + providerDefinition, typeElement);
                    }
                    return typeElement;
                })
                .toList();
    }
}
