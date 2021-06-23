package vest.doctor.jpa;

import vest.doctor.AdHocProvider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.AppLoader;
import vest.doctor.CodeProcessingException;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EntityManagerProviderListener implements ProviderDefinitionListener {

    private final Set<String> processedPersistenceUnits = new HashSet<>();

    private ClassBuilder jpaAppLoader;
    private MethodBuilder preProcess;

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        PersistenceContext[] persistenceContexts = providerDefinition.annotationSource().getAnnotationsByType(PersistenceContext.class);
        if (persistenceContexts != null) {
            for (PersistenceContext persistenceContext : persistenceContexts) {
                processAnnotation(context, persistenceContext);
            }
        }
    }

    private void processAnnotation(AnnotationProcessorContext context, PersistenceContext persistenceContext) {
        String pcName = Objects.requireNonNull(persistenceContext.name(), "@PersistenceContext annotations must have a name defined that matches the persistence unit name in the xml");

        if (processedPersistenceUnits.contains(pcName)) {
            throw new CodeProcessingException("multiple @PersistenceContext annotations with the same name: " + pcName);
        }
        processedPersistenceUnits.add(pcName);
        initAppLoader(context);

        String properties = "properties" + context.nextId();
        String emf = "entityManagerFactory" + context.nextId();
        String em = "entityManager" + context.nextId();
        preProcess.line("Map<String, String> ", properties, " = new LinkedHashMap<>();");
        for (PersistenceProperty property : persistenceContext.properties()) {
            preProcess.line(properties, ".put({{providerRegistry}}.resolvePlaceholders(\"", property.name(), "\"), {{providerRegistry}}.resolvePlaceholders(\"", property.value(), "\"));");
        }
        preProcess.line("EntityManagerFactory ", emf, " = Persistence.createEntityManagerFactory({{providerRegistry}}.resolvePlaceholders(\"", pcName, "\"), ", properties, ");");
        preProcess.line("{{providerRegistry}}.shutdownContainer().register(", emf, "::close);");
        preProcess.line("{{providerRegistry}}.register(new AdHocProvider<>(EntityManagerFactory.class, ", emf, ", \"", ProcessorUtils.escapeStringForCode(pcName), "\"));");

        preProcess.line("EntityManager ", em, " = null;");
        preProcess.line("try{");
        preProcess.line(em, " = ", emf, ".createEntityManager(SynchronizationType.", persistenceContext.synchronization(), ", ", properties, ");");
        preProcess.line("} catch (" + IllegalStateException.class.getSimpleName() + " e) {");
        preProcess.line("log.warn(\"could not create entity manager with explicit synchronization type, falling back; error message: {}\", e.getMessage());");
        preProcess.line("log.debug(\"full error stack\", e);");
        preProcess.line(em, " = ", emf, ".createEntityManager(", properties, ");");
        preProcess.line("}");
        preProcess.line("{{providerRegistry}}.shutdownContainer().register(", em, "::close);");
        preProcess.line("{{providerRegistry}}.register(new AdHocProvider<>(EntityManager.class, ", em, ", \"", ProcessorUtils.escapeStringForCode(pcName), "\"));");
        context.addSatisfiedDependency(EntityManagerFactory.class, '"' + pcName + '"');
        context.addSatisfiedDependency(EntityManager.class, '"' + pcName + '"');
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        if (jpaAppLoader != null) {
            jpaAppLoader.writeClass(context.filer());
            context.addServiceImplementation(AppLoader.class, jpaAppLoader.getFullyQualifiedClassName());
        }
    }

    private void initAppLoader(AnnotationProcessorContext context) {
        if (jpaAppLoader != null) {
            return;
        }
        String generatedClassName = context.generatedPackage() + ".JPAAppLoader__" + context.nextId();
        jpaAppLoader = new ClassBuilder()
                .setClassName(generatedClassName)
                .addImplementsInterface(AppLoader.class)
                .addImportClass(EntityManager.class)
                .addImportClass(EntityManagerFactory.class)
                .addImportClass(Persistence.class)
                .addImportClass(SynchronizationType.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(AdHocProvider.class)
                .addImportClass(Map.class)
                .addImportClass(LinkedHashMap.class)
                .addImportClass("org.slf4j.Logger")
                .addImportClass("org.slf4j.LoggerFactory")
                .addField("private final static Logger log = LoggerFactory.getLogger(", generatedClassName, ".class)");

        preProcess = jpaAppLoader.newMethod("public void preProcess(ProviderRegistry {{providerRegistry}})");
    }
}
