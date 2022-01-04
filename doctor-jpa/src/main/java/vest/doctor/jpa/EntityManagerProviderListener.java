package vest.doctor.jpa;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;

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
    private MethodBuilder stage2;

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
        stage2.line("Map<String, String> ", properties, " = new LinkedHashMap<>();");
        for (PersistenceProperty property : persistenceContext.properties()) {
            stage2.line(properties, ".put({{providerRegistry}}.resolvePlaceholders(\"", property.name(), "\"), {{providerRegistry}}.resolvePlaceholders(\"", property.value(), "\"));");
        }
        stage2.line("EntityManagerFactory ", emf, " = Persistence.createEntityManagerFactory({{providerRegistry}}.resolvePlaceholders(\"", pcName, "\"), ", properties, ");");
        stage2.line("{{providerRegistry}}.register(new AdHocProvider<>(EntityManagerFactory.class, ", emf, ", \"", ProcessorUtils.escapeStringForCode(pcName), "\",", emf, "::close));");

        stage2.line("EntityManager ", em, " = null;");
        stage2.line("try{");
        stage2.line(em, " = ", emf, ".createEntityManager(SynchronizationType.", persistenceContext.synchronization(), ", ", properties, ");");
        stage2.line("} catch (", IllegalStateException.class.getSimpleName(), " e) {");
        stage2.line("log.warn(\"could not create entity manager with explicit synchronization type, falling back; error message: {}\", e.getMessage());");
        stage2.line("log.debug(\"full error stack\", e);");
        stage2.line(em, " = ", emf, ".createEntityManager(", properties, ");");
        stage2.line("}");
        stage2.line("{{providerRegistry}}.register(new AdHocProvider<>(EntityManager.class, ", em, ", \"", ProcessorUtils.escapeStringForCode(pcName), "\",", em, "::close));");
        context.addSatisfiedDependency(EntityManagerFactory.class, '"' + pcName + '"');
        context.addSatisfiedDependency(EntityManager.class, '"' + pcName + '"');
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        if (jpaAppLoader != null) {
            jpaAppLoader.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, jpaAppLoader.getFullyQualifiedClassName());
            jpaAppLoader = null;
        }
    }

    private void initAppLoader(AnnotationProcessorContext context) {
        if (jpaAppLoader != null) {
            return;
        }
        String generatedClassName = context.generatedPackage() + ".JPALoader__" + context.nextId();
        jpaAppLoader = new ClassBuilder()
                .setClassName(generatedClassName)
                .addImplementsInterface(ApplicationLoader.class)
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

        stage2 = jpaAppLoader.newMethod("public void stage2(ProviderRegistry {{providerRegistry}})");
    }
}
