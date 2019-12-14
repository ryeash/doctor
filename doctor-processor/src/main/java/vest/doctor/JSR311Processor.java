package vest.doctor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Scope;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JSR311Processor extends AbstractProcessor implements AnnotationProcessorContext {

    public static final String GENERATED_PACKAGE = "vest.doctor.generated.$" + ProcessorUtils.uniqueHash();
    public static final String APP_LOADER_CLASS_NAME = GENERATED_PACKAGE + ".AppLoaderImpl";

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private ProcessingEnvironment processingEnv;

    private final List<ProviderDefinitionProcessor> providerDefinitionProcessors = new LinkedList<>();
    private final Map<Class<? extends Annotation>, ScopeWriter> scopeWriters = new HashMap<>();
    private final List<ProviderWrappingWriter> wrappingWriters = new LinkedList<>();
    private final List<ProviderDefinition> providerDefinitions = new LinkedList<>();
    private final List<NewInstanceCustomizer> newInstanceCustomizers = new LinkedList<>();
    private final List<ParameterLookupCustomizer> parameterLookupCustomizers = new LinkedList<>();
    private final List<TypeElement> scopesToProcess = new LinkedList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;

        // add default scopes
        scopesToProcess.add(processingEnv.getElementUtils().getTypeElement(Prototype.class.getCanonicalName()));
        scopesToProcess.add(processingEnv.getElementUtils().getTypeElement(Singleton.class.getCanonicalName()));
        scopesToProcess.add(processingEnv.getElementUtils().getTypeElement(ThreadLocal.class.getCanonicalName()));
        scopesToProcess.add(processingEnv.getElementUtils().getTypeElement(Cached.class.getCanonicalName()));

        providerDefinitionProcessors.add(new ConstructorProviderDefinitionProcessor());
        providerDefinitionProcessors.add(new FactoryProviderDefinitionProcessor());
        for (ProviderDefinitionProcessor providerDefinitionProcessor : ServiceLoader.load(ProviderDefinitionProcessor.class)) {
            providerDefinitionProcessors.add(providerDefinitionProcessor);
        }
        providerDefinitionProcessors.sort(Prioritized.COMPARATOR);

        ScopeWriter singleton = new SingletonScopeWriter();
        ScopeWriter threadLocal = new ThreadLocalScopeWriter();
        ScopeWriter cached = new CachedScopeWriter();
        ScopeWriter prototype = new PrototypeScopeWriter();
        scopeWriters.put(singleton.scope(), singleton);
        scopeWriters.put(threadLocal.scope(), threadLocal);
        scopeWriters.put(cached.scope(), cached);
        scopeWriters.put(prototype.scope(), prototype);
        for (ScopeWriter scopeWriter : ServiceLoader.load(ScopeWriter.class)) {
            if (scopeWriters.containsKey(scopeWriter.scope())) {
                throw new RuntimeException("error: multiple scopes registered for: " + scopeWriter.scope());
            }
            scopeWriters.put(scopeWriter.scope(), scopeWriter);
        }

        wrappingWriters.add(new ShutdownWrappingWriter());
        for (ProviderWrappingWriter providerWrappingWriter : ServiceLoader.load(ProviderWrappingWriter.class)) {
            wrappingWriters.add(providerWrappingWriter);
        }
        wrappingWriters.sort(Prioritized.COMPARATOR);

        newInstanceCustomizers.add(new InjectMethodsCustomizer());
        newInstanceCustomizers.add(new EventManagerWriter());
        newInstanceCustomizers.add(new ScheduledMethodCustomizer());
        parameterLookupCustomizers.add(new PropertyParameterCustomizer());
        parameterLookupCustomizers.add(new ProviderParameterLookupCustomizer());
        for (CustomizationPoint customizationPoint : ServiceLoader.load(CustomizationPoint.class)) {
            boolean known = false;
            if (customizationPoint instanceof NewInstanceCustomizer) {
                newInstanceCustomizers.add((NewInstanceCustomizer) customizationPoint);
                known = true;
            }
            if (customizationPoint instanceof ParameterLookupCustomizer) {
                parameterLookupCustomizers.add((ParameterLookupCustomizer) customizationPoint);
                known = true;
            }
            if (!known) {
                errorMessage("unhandled customization: " + customizationPoint);
            }
        }
        newInstanceCustomizers.sort(Prioritized.COMPARATOR);
        parameterLookupCustomizers.sort(Prioritized.COMPARATOR);
    }

    private final Set<Element> processedElements = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getAnnotation(Scope.class) != null && !scopesToProcess.contains(annotation)) {
                scopesToProcess.add(annotation);
            }
        }

        Stream.of(scopesToProcess, annotations)
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
            Stream.of(newInstanceCustomizers, parameterLookupCustomizers)
                    .flatMap(Collection::stream)
                    .forEach(c -> c.finish(this));
            compileTimeDependencyCheck();
            writeAppLoaderImplementation();
            writeServicesResource();
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(Singleton.class, ThreadLocal.class, Prototype.class, Cached.class, Factory.class)
                .map(Class::getCanonicalName)
                .collect(Collectors.toSet());
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

    @Override
    public ProcessingEnvironment processingEnvironment() {
        return processingEnv;
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

    private void writeAppLoaderImplementation() {
        ClassBuilder cb = new ClassBuilder();
        cb.addField("private final List<DoctorProvider<?>> eagerList = new ArrayList<>()");

        MethodBuilder mb = new MethodBuilder("public void load(BeanProvider beanProvider)");

        for (ProviderDefinition providerDefinition : providerDefinitions) {

            cb.addImportClass(providerDefinition.providedType().asType().toString());
            cb.addField("private DoctorProvider<" + providerDefinition.providedType().getSimpleName() + "> " + providerDefinition.uniqueInstanceName());

            String creator = providerDefinition.initializationCode("beanProvider");

            for (ProviderWrappingWriter wrappingWriter : wrappingWriters) {
                creator = wrappingWriter.wrap(this, providerDefinition, creator);
            }

            boolean hasModules = !providerDefinition.modules().isEmpty();
            if (hasModules) {
                String modules = providerDefinition.modules()
                        .stream()
                        .filter(Objects::nonNull)
                        .map(m -> '"' + m + '"')
                        .collect(Collectors.joining(", ", "(", ")"));
                mb.line("if(isActive(beanProvider, java.util.Arrays.asList" + modules + ")){");
            }

            if (providerDefinition.scope() != null) {
                Class<?> scopeType = loadClass(providerDefinition.scope().getAnnotationType().toString());
                ScopeWriter scopeWriter = scopeWriters.get(scopeType);
                if (scopeWriter == null) {
                    throw new IllegalArgumentException("unsupported scope type: " + scopeType);
                }
                creator = scopeWriter.wrap(this, providerDefinition, creator);
            }
            mb.line(providerDefinition.uniqueInstanceName() + " = " + creator + ";");
            mb.line("beanProvider.register(" + providerDefinition.uniqueInstanceName() + ");");
            if (providerDefinition.isPrimary()) {
                mb.line("beanProvider.register(new " + PrimaryProviderWrapper.class.getCanonicalName() + "(" + providerDefinition.uniqueInstanceName() + "));");
            }

            if (providerDefinition.isEager()) {
                mb.line("eagerList.add(" + providerDefinition.uniqueInstanceName() + ");");
            }

            if (hasModules) {
                mb.line("}");
            }
        }

        cb.setClassName(APP_LOADER_CLASS_NAME)
                .addImplementsInterface(AppLoader.class)
                .addImportClass(List.class)
                .addImportClass(ArrayList.class)
                .addImportClass(Objects.class)
                .addImportClass(BeanProvider.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(ShutdownContainer.class)
                .addField("private final ShutdownContainer shutdownContainer = new ShutdownContainer()")
                .addMethod(mb.finish())
                // call eager providers
                .addMethod("public void postProcess(BeanProvider beanProvider) { eagerList.stream().filter(Objects::nonNull).forEach(DoctorProvider::get); }")
                .addMethod("public void close() { shutdownContainer.close(); }")
                .writeClass(filer());
    }

    private void writeServicesResource() {
        try {
            FileObject sourceFile = filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/vest.doctor.AppLoader");
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(APP_LOADER_CLASS_NAME);
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
        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(EventProducer.class.getCanonicalName());
        ProviderDependency eventProducerDependency = buildDependency(typeElement, null, false);

        for (Map.Entry<ProviderDependency, Set<ProviderDependency>> entry : typesToDependencies.entrySet()) {
            ProviderDependency target = entry.getKey();
            for (ProviderDependency dependency : entry.getValue()) {
                if (Objects.equals(eventProducerDependency, dependency)) {
                    continue;
                }
                if (dependency != null && dependency.required() && !isProvided(dependency)) {
                    errorMessage("missing provider dependency for target:" + target + "-> dependency" + dependency + " -- " +
                            providerDefinitions.stream().map(ProviderDefinition::asDependency).map(String::valueOf).collect(Collectors.joining("\n")));
                }
            }
        }
    }
}
