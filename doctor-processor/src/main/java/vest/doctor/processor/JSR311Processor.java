package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.AppLoader;
import vest.doctor.ClassBuilder;
import vest.doctor.ConfigurationFacade;
import vest.doctor.Constants;
import vest.doctor.CustomizationPoint;
import vest.doctor.DoctorProvider;
import vest.doctor.EventManager;
import vest.doctor.EventProducer;
import vest.doctor.MethodBuilder;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ParameterLookupCustomizer;
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
import vest.doctor.StringConversionGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Provider;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vest.doctor.Constants.PROVIDER_REGISTRY;
import static vest.doctor.Line.line;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({JSR311Processor.PACKAGE_NAME_OPTION})
public class JSR311Processor extends AbstractProcessor implements AnnotationProcessorContext {

    public static final String PACKAGE_NAME_OPTION = "doctor.generated.packagename";

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private ProcessingEnvironment processingEnv;
    private String generatedPackage;
    private final List<TypeElement> annotationsToProcess = new LinkedList<>();
    private final List<ProviderDefinitionProcessor> providerDefinitionProcessors = new LinkedList<>();
    private final Map<Class<? extends Annotation>, ScopeWriter> scopeWriters = new HashMap<>();
    private final List<ProviderCustomizationPoint> providerCustomizationPoints = new LinkedList<>();
    private final List<NewInstanceCustomizer> newInstanceCustomizers = new LinkedList<>();
    private final List<ParameterLookupCustomizer> parameterLookupCustomizers = new LinkedList<>();
    private final List<StringConversionGenerator> stringConversionGenerators = new LinkedList<>();
    private final List<ProviderDefinition> providerDefinitions = new LinkedList<>();
    private final List<ProviderDefinitionListener> providerDefinitionListeners = new LinkedList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;

        this.generatedPackage = "vest.doctor.generated."
                + processingEnv.getOptions().getOrDefault(PACKAGE_NAME_OPTION, "$" + ProcessorUtils.uniqueHash());

        loadConf(new DefaultProcessorConfiguration());
        for (ProcessorConfiguration processorConfiguration : ServiceLoader.load(ProcessorConfiguration.class, JSR311Processor.class.getClassLoader())) {
            loadConf(processorConfiguration);
        }
        providerDefinitionProcessors.sort(Prioritized.COMPARATOR);
        providerCustomizationPoints.sort(Prioritized.COMPARATOR);
        newInstanceCustomizers.sort(Prioritized.COMPARATOR);
        parameterLookupCustomizers.sort(Prioritized.COMPARATOR);
        stringConversionGenerators.sort(Prioritized.COMPARATOR);
    }

    private void loadConf(ProcessorConfiguration processorConfiguration) {
        for (Class<? extends Annotation> supportedAnnotation : processorConfiguration.supportedAnnotations()) {
            annotationsToProcess.add(processingEnv.getElementUtils().getTypeElement(supportedAnnotation.getCanonicalName()));
        }

        providerDefinitionProcessors.addAll(processorConfiguration.providerDefinitionProcessors());

        for (CustomizationPoint customizationPoint : processorConfiguration.customizationPoints()) {
            boolean known = false;
            if (customizationPoint instanceof ScopeWriter) {
                ScopeWriter sw = (ScopeWriter) customizationPoint;
                if (scopeWriters.containsKey(sw.scope())) {
                    throw new RuntimeException("error: multiple scopes registered for: " + sw.scope());
                }
                scopeWriters.put(sw.scope(), sw);
                known = true;
            }
            if (customizationPoint instanceof ProviderCustomizationPoint) {
                providerCustomizationPoints.add((ProviderCustomizationPoint) customizationPoint);
                known = true;
            }
            if (customizationPoint instanceof NewInstanceCustomizer) {
                newInstanceCustomizers.add((NewInstanceCustomizer) customizationPoint);
                known = true;
            }
            if (customizationPoint instanceof ParameterLookupCustomizer) {
                parameterLookupCustomizers.add((ParameterLookupCustomizer) customizationPoint);
                known = true;
            }
            if (customizationPoint instanceof StringConversionGenerator) {
                stringConversionGenerators.add((StringConversionGenerator) customizationPoint);
                known = true;
            }
            if (customizationPoint instanceof ProviderDefinitionListener) {
                providerDefinitionListeners.add((ProviderDefinitionListener) customizationPoint);
                known = true;
            }
            if (!known) {
                errorMessage("unhandled customization: " + customizationPoint);
            }
        }
    }

    private final Set<Element> processedElements = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Stream.of(annotationsToProcess, annotations)
                .flatMap(Collection::stream)
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Collection::stream)
                .filter(processedElements::add)
                .forEach(annotatedElement -> {
                    boolean claimed = false;
                    for (ProviderDefinitionProcessor providerDefinitionProcessor : providerDefinitionProcessors) {
                        ProviderDefinition provDef = providerDefinitionProcessor.process(this, annotatedElement);
                        if (provDef != null) {
                            errorChecking(provDef);
                            claimed = true;
                            typesToDependencies.computeIfAbsent(provDef.asDependency(), d -> new HashSet<>());
                            providerDefinitions.add(provDef);
                            for (ProviderDefinitionListener providerDefinitionListener : providerDefinitionListeners) {
                                providerDefinitionListener.process(this, provDef);
                            }
                            break;
                        }
                    }
                    if (!claimed) {
                        warnMessage("the annotated element " + ProcessorUtils.debugString(annotatedElement) + " was not claimed by a processor");
                    }
                });

        if (roundEnv.processingOver()) {
            for (ProviderDefinitionProcessor providerDefinitionProcessor : providerDefinitionProcessors) {
                providerDefinitionProcessor.finish(this);
            }
            writeAppLoaderImplementation();
            Stream.of(newInstanceCustomizers, parameterLookupCustomizers, providerCustomizationPoints, providerDefinitionListeners)
                    .flatMap(Collection::stream)
                    .forEach(c -> c.finish(this));
            writeServicesResource();
            compileTimeDependencyCheck();
        }
        return true;
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
        for (ProviderDefinition providerDefinition : providerDefinitions) {
            for (TypeElement type : providerDefinition.getAllProvidedTypes()) {
                Dependency provided = new Dependency(type, providerDefinition.qualifier());
                if (Objects.equals(dependency, provided)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<NewInstanceCustomizer> newInstanceCustomizers() {
        return Collections.unmodifiableList(newInstanceCustomizers);
    }

    @Override
    public List<ParameterLookupCustomizer> parameterLookupCustomizers() {
        return Collections.unmodifiableList(parameterLookupCustomizers);
    }

    @Override
    public Number nextId() {
        return idGenerator.incrementAndGet();
    }

    private final Map<ProviderDependency, Set<ProviderDependency>> typesToDependencies = new HashMap<>();

    @Override
    public void registerDependency(ProviderDependency target, ProviderDependency dependency) {
        if (target == null) {
            errorMessage("cannot register dependency for null target");
        }
        if (dependency == null) {
            errorMessage("cannot register null dependency for " + target);
        }
        typesToDependencies.computeIfAbsent(target, t -> new HashSet<>()).add(dependency);
    }

    @Override
    public ProviderDependency buildDependency(TypeElement type, String qualifier, boolean required) {
        return new Dependency(type, qualifier, required);
    }

    @Override
    public <T extends CustomizationPoint> List<T> customizations(Class<T> type) {
        return Stream.of(providerDefinitionProcessors,
                providerCustomizationPoints,
                newInstanceCustomizers,
                parameterLookupCustomizers,
                stringConversionGenerators,
                providerDefinitionListeners)
                .flatMap(Collection::stream)
                .filter(type::isInstance)
                .distinct()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    private void errorChecking(ProviderDefinition providerDefinition) {
        for (VariableElement field : providerDefinition.fields(Inject.class)) {
            errorMessage("field injection is not supported: " + ProcessorUtils.debugString(field));
        }
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PreDestroy", preDestroy -> {
            for (ExecutableElement method : providerDefinition.methods(preDestroy)) {
                errorMessage("@PreDestroy is not supported (use the AutoCloseable interface instead): " + ProcessorUtils.debugString(method));
            }
        });
    }

    private void writeAppLoaderImplementation() {
        ClassBuilder cb = new ClassBuilder()
                .setClassName(generatedPackage + ".AppLoaderImpl")
                .addImplementsInterface(AppLoader.class)
                .addImportClass(List.class)
                .addImportClass(ArrayList.class)
                .addImportClass(Objects.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(PrimaryProviderWrapper.class)
                .addImportClass(ShutdownContainer.class)
                .addField(line("private final {} {} = new {}()", ShutdownContainer.class, Constants.SHUTDOWN_CONTAINER_NAME, ShutdownContainer.class))
                // call eager providers
                .addMethod(line("public void postProcess({} {}) { eagerList.stream().filter(Objects::nonNull).forEach({}::get); }", ProviderRegistry.class, PROVIDER_REGISTRY, DoctorProvider.class))
                .addMethod(line("public void close() { {}.close(); }", Constants.SHUTDOWN_CONTAINER_NAME));

        cb.addField(line("private final List<{}<?>> eagerList = new ArrayList<>()", DoctorProvider.class));

        MethodBuilder load = new MethodBuilder(line("public void load({} {})", ProviderRegistry.class, PROVIDER_REGISTRY));

        for (ProviderDefinition providerDefinition : providerDefinitions) {
            cb.addNestedClass(providerDefinition.getClassBuilder());

            cb.addImportClass(providerDefinition.providedType().asType().toString());

            String creator = line("new {}({})", providerDefinition.generatedClassName(), PROVIDER_REGISTRY);

            for (ProviderCustomizationPoint providerCustomizationPoint : providerCustomizationPoints) {
                creator = providerCustomizationPoint.wrap(this, providerDefinition, creator, PROVIDER_REGISTRY);
            }

            boolean hasModules = !providerDefinition.modules().isEmpty();
            if (hasModules) {
                String modules = providerDefinition.modules()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(m -> '"' + m + '"')
                        .collect(Collectors.joining(", "));
                if (providerDefinition.modules().size() == 1) {
                    load.line("if(isActive({}, java.util.Collections.singletonList({}))){", PROVIDER_REGISTRY, modules);
                } else {
                    load.line("if(isActive({}, java.util.Arrays.asList({}))){", PROVIDER_REGISTRY, modules);
                }
            }

            if (providerDefinition.scope() != null) {
                Class<?> scopeType = loadClass(providerDefinition.scope().getAnnotationType().toString());
                ScopeWriter scopeWriter = scopeWriters.get(scopeType);
                if (scopeWriter == null) {
                    throw new IllegalArgumentException("unsupported scope type: " + scopeType);
                }
                creator = scopeWriter.wrapScope(this, providerDefinition, creator);
            }
            load.line("{}<{}> {} = {};", DoctorProvider.class, providerDefinition.providedType().getSimpleName(), providerDefinition.uniqueInstanceName(), creator);
            load.line("{}.register({});", PROVIDER_REGISTRY, providerDefinition.uniqueInstanceName());
            if (providerDefinition.isPrimary()) {
                load.line("{}.register(new {}({}));", PROVIDER_REGISTRY, PrimaryProviderWrapper.class, providerDefinition.uniqueInstanceName());
            }

            if (providerDefinition.isEager()) {
                load.line("eagerList.add({});", providerDefinition.uniqueInstanceName());
            }

            if (hasModules) {
                load.line("}");
            }
        }

        cb.addMethod(load.finish())
                .writeClass(filer());
    }

    private void writeServicesResource() {
        try {
            FileObject sourceFile = filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/vest.doctor.AppLoader");
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(generatedPackage + ".AppLoaderImpl");
            }
        } catch (IOException e) {
            errorMessage("error writing services resources");
            e.printStackTrace();
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            errorMessage("error loading class: " + className);
            throw new NullPointerException("unreachable");
        }
    }

    private void compileTimeDependencyCheck() {
        List<ProviderDependency> builtins = ignoredBuiltins(ProviderRegistry.class, ConfigurationFacade.class, EventProducer.class, EventManager.class);
        for (Map.Entry<ProviderDependency, Set<ProviderDependency>> entry : typesToDependencies.entrySet()) {
            ProviderDependency target = entry.getKey();
            for (ProviderDependency dependency : entry.getValue()) {
                if (builtins.contains(dependency)) {
                    continue;
                }
                if (dependency != null && dependency.required() && !isProvided(dependency)) {
                    errorMessage("missing provider dependency for\ntarget:" + target + "\ndependency:" + dependency + "\nknown types:\n" +
                            providerDefinitions.stream().map(ProviderDefinition::asDependency).map(String::valueOf).collect(Collectors.joining("\n")));
                }
            }
        }
    }

    private List<ProviderDependency> ignoredBuiltins(Class<?>... types) {
        return Stream.of(types)
                .map(Class::getCanonicalName)
                .map(processingEnv.getElementUtils()::getTypeElement)
                .map(t -> buildDependency(t, null, false))
                .collect(Collectors.toList());
    }
}
