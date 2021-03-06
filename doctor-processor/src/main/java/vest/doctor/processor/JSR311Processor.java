package vest.doctor.processor;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ApplicationLoader;
import vest.doctor.CodeProcessingException;
import vest.doctor.ConfigurationFacade;
import vest.doctor.CustomizationPoint;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Primary;
import vest.doctor.PrimaryProviderWrapper;
import vest.doctor.Prioritized;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderDefinitionProcessor;
import vest.doctor.ProviderDependency;
import vest.doctor.ProviderRegistry;
import vest.doctor.ScopeWriter;
import vest.doctor.ShutdownContainer;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.event.EventProducer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vest.doctor.codegen.Constants.PROVIDER_REGISTRY;

@SupportedSourceVersion(SourceVersion.RELEASE_15)
@SupportedOptions({JSR311Processor.PACKAGE_NAME_OPTION})
public class JSR311Processor extends AbstractProcessor implements AnnotationProcessorContext {

    public static final String PACKAGE_NAME_OPTION = "doctor.generated.packagename";
    private static final AtomicInteger idGenerator = new AtomicInteger();

    private ProcessingEnvironment processingEnv;
    private String generatedPackage;
    private final List<TypeElement> annotationsToProcess = new LinkedList<>();

    private final List<CustomizationPoint> customizationPoints = new LinkedList<>();
    private final List<ProviderDefinition> providerDefinitions = new LinkedList<>();

    private final Set<ProviderDependency> additionalSatisfiedDependencies = new HashSet<>();

    private long start;

    private ClassBuilder appLoader;
    private MethodBuilder stage3;
    private MethodBuilder stage5;

    private final Map<Class<?>, Collection<String>> serviceImplementations = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.start = System.currentTimeMillis();
        super.init(processingEnv);
        this.processingEnv = processingEnv;

        this.generatedPackage = "vest.doctor.generated."
                + processingEnv.getOptions().getOrDefault(PACKAGE_NAME_OPTION, "$" + ProcessorUtils.uniqueHash());

        loadConf(new DefaultProcessorConfiguration());
        for (ProcessorConfiguration processorConfiguration : ServiceLoader.load(ProcessorConfiguration.class, JSR311Processor.class.getClassLoader())) {
            loadConf(processorConfiguration);
        }
        customizationPoints.sort(Prioritized.COMPARATOR);

        addSatisfiedDependency(ProviderRegistry.class, null);
        addSatisfiedDependency(ConfigurationFacade.class, null);
        addSatisfiedDependency(EventProducer.class, null);

        this.appLoader = new ClassBuilder()
                .setClassName(generatedPackage + ".AppLoaderImpl")
                .addImplementsInterface(ApplicationLoader.class)
                .addImportClass(List.class)
                .addImportClass(ArrayList.class)
                .addImportClass(Objects.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(PrimaryProviderWrapper.class)
                .addImportClass(ShutdownContainer.class)
                .addField("private final List<", DoctorProvider.class, "<?>> eagerList = new ArrayList<>()");
        this.stage3 = appLoader.newMethod("public void stage3(", ProviderRegistry.class, " {{providerRegistry}})");
        this.stage5 = appLoader.newMethod("public void stage5(", ProviderRegistry.class, " {{providerRegistry}})");
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
        Stream.of(annotationsToProcess, annotations)
                .flatMap(Collection::stream)
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(processedElements::add)
                .forEach(this::processElement);

        if (roundEnv.processingOver()) {
            customizationPoints.forEach(c -> c.finish(this));
            stage5.line("eagerList.stream().filter(Objects::nonNull).forEach(", Provider.class, "::get);");
            appLoader.writeClass(filer());
            addServiceImplementation(ApplicationLoader.class, appLoader.getFullyQualifiedClassName());
            writeServicesResource();
            compileTimeDependencyCheck();
            infoMessage("took " + (System.currentTimeMillis() - start) + "ms");
        }
        return annotationsToProcess.containsAll(annotations);
    }

    private void processElement(Element annotatedElement) {
        try {
            boolean claimed = false;
            for (ProviderDefinitionProcessor providerDefinitionProcessor : customizations(ProviderDefinitionProcessor.class)) {
                ProviderDefinition provDef = providerDefinitionProcessor.process(this, annotatedElement);
                if (provDef != null) {
                    errorChecking(provDef);
                    claimed = true;
                    typesToDependencies.computeIfAbsent(provDef.asDependency(), d -> new HashSet<>());
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
            if (!claimed) {
                warnMessage("the annotated element " + ProcessorUtils.debugString(annotatedElement) + " was not claimed by a processor");
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

    private final Map<ProviderDependency, Set<ProviderDependency>> typesToDependencies = new HashMap<>();

    @Override
    public void registerDependency(ProviderDependency target, ProviderDependency dependency) {
        if (target == null) {
            throw new CodeProcessingException("cannot register dependency for null target");
        }
        if (dependency == null) {
            throw new CodeProcessingException("cannot register null dependency for " + target);
        }
        typesToDependencies.computeIfAbsent(target, t -> new HashSet<>()).add(dependency);
    }

    @Override
    public void addSatisfiedDependency(Class<?> type, String qualifier) {
        additionalSatisfiedDependencies.add(buildDependency(processingEnv.getElementUtils().getTypeElement(type.getCanonicalName()), qualifier, false));
    }

    @Override
    public ProviderDependency buildDependency(TypeElement type, String qualifier, boolean required) {
        return new Dependency(type, qualifier, required);
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

    private void errorChecking(ProviderDefinition providerDefinition) {
        for (VariableElement variableElement : ProcessorUtils.allFields(this, providerDefinition.providedType())) {
            if (variableElement.getAnnotation(Inject.class) != null) {
                throw new CodeProcessingException("field injection is not supported", variableElement);
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
        appLoader.addImportClass(providerDefinition.providedType().asType().toString());

        String creator = "new " + providerDefinition.generatedClassName() + "({{providerRegistry}})";

        for (ProviderCustomizationPoint providerCustomizationPoint : customizations(ProviderCustomizationPoint.class)) {
            creator = providerCustomizationPoint.wrap(this, providerDefinition, creator, PROVIDER_REGISTRY);
        }

        boolean hasModules = !providerDefinition.modules().isEmpty();
        if (hasModules) {
            String modules = providerDefinition.modules()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(m -> '"' + m + '"')
                    .collect(Collectors.joining(", "));
            stage3.line("if(isActive({{providerRegistry}}, List.of(", modules, "))){");
        }

        if (providerDefinition.scope() != null) {
            Class<?> scopeType = loadClass(providerDefinition.scope().getAnnotationType().toString());
            ScopeWriter scopeWriter = getScopeWriter(scopeType);
            creator = scopeWriter.wrapScope(this, providerDefinition, creator);
        }
        stage3.line(DoctorProvider.class, "<", providerDefinition.providedType().getSimpleName(), "> ", providerDefinition.uniqueInstanceName(), " = ", creator, ";");
        stage3.line("{{providerRegistry}}.register(", providerDefinition.uniqueInstanceName(), ");");
        if (providerDefinition.markedWith(Primary.class)) {
            if (providerDefinition.qualifier() == null) {
                throw new IllegalArgumentException("unqualified provider can not be marked @Primary: " + providerDefinition);
            }
            stage3.line("{{providerRegistry}}.register(new ", PrimaryProviderWrapper.class, "(", providerDefinition.uniqueInstanceName(), "));");
        }

        if (providerDefinition.markedWith(Eager.class)) {
            stage3.line("eagerList.add(", providerDefinition.uniqueInstanceName(), ");");
        }

        if (hasModules) {
            stage3.line("}");
        }
    }

    private ScopeWriter getScopeWriter(Class<?> scopeType) {
        for (ScopeWriter scopeWriter : customizations(ScopeWriter.class)) {
            if (scopeWriter.scope().equals(scopeType)) {
                return scopeWriter;
            }
        }
        throw new IllegalArgumentException("unsupported scope type: " + scopeType);
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

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new CodeProcessingException("error loading class: " + className);
        }
    }

    private void compileTimeDependencyCheck() {
        for (Map.Entry<ProviderDependency, Set<ProviderDependency>> entry : typesToDependencies.entrySet()) {
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
}
