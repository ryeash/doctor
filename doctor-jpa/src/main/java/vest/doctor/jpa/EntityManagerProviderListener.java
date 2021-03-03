package vest.doctor.jpa;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.DestroyMethod;
import vest.doctor.Factory;
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
        initAppLoader(context);
        String pcName = Objects.requireNonNull(persistenceContext.name(), "@PersistenceContext annotations must have a name defined that matches the persistence unit name in the xml");

        if (processedPersistenceUnits.contains(pcName)) {
            context.errorMessage("multiple @PersistenceContext annotations with the same name: " + pcName);
            return;
        }
        processedPersistenceUnits.add(pcName);

        String generatedClassName = context.generatedPackage() + ".JPAObjectFactory__jpa" + context.nextId();
        ClassBuilder entityManagerFactory = new ClassBuilder()
                .setClassName(generatedClassName)
                .addClassAnnotation("@Singleton")
                .addImportClass(Singleton.class)
                .addImportClass(Named.class)
                .addImportClass(Factory.class)
                .addImportClass(DestroyMethod.class)
                .addImportClass(EntityManager.class)
                .addImportClass(EntityManagerFactory.class)
                .addImportClass(Persistence.class)
                .addImportClass(SynchronizationType.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Map.class)
                .addImportClass(LinkedHashMap.class)
                .addImportClass("org.slf4j.Logger")
                .addImportClass("org.slf4j.LoggerFactory");

        entityManagerFactory.addField("private final static Logger log = LoggerFactory.getLogger(", generatedClassName, ".class)");

        MethodBuilder emf = entityManagerFactory.newMethod("@Singleton @Factory @DestroyMethod(\"close\") @Named(\"", ProcessorUtils.escapeStringForCode(pcName), "\") " +
                "public " + EntityManagerFactory.class.getSimpleName() + " entityManagerFactory" + context.nextId() + "(" + ProviderRegistry.class.getSimpleName() + " {{providerRegistry}})");
        emf.line("Map<String, String> properties = new LinkedHashMap<>();");
        for (PersistenceProperty property : persistenceContext.properties()) {
            emf.line("properties.put({{providerRegistry}}.resolvePlaceholders(\"", property.name(), "\"), {{providerRegistry}}.resolvePlaceholders(\"", property.value(), "\"));");
        }
        emf.line("return Persistence.createEntityManagerFactory({{providerRegistry}}.resolvePlaceholders(\"", pcName, "\"), properties);");

        MethodBuilder em = entityManagerFactory.newMethod("@Singleton @Factory @DestroyMethod(\"close\") @Named(\"", ProcessorUtils.escapeStringForCode(pcName), "\") ",
                "public ", EntityManager.class.getSimpleName(), " entityManager", context.nextId(), "(", ProviderRegistry.class, " {{providerRegistry}}, @Named(\"", ProcessorUtils.escapeStringForCode(pcName), "\") EntityManagerFactory entityManagerFactory)");
        em.line("try{");
        em.line("return entityManagerFactory.createEntityManager(SynchronizationType.", persistenceContext.synchronization(), ", entityManagerFactory.getProperties());");
        em.line("} catch (" + IllegalStateException.class.getSimpleName() + " e) {");
        em.line("log.warn(\"could not create entity manager with explicit synchronization type, falling back; error message: {}\", e.getMessage());");
        em.line("log.debug(\"full error stack\", e);");
        em.line("return entityManagerFactory.createEntityManager(entityManagerFactory.getProperties());");
        em.line("}");
        entityManagerFactory.writeClass(context.filer());
    }

    private void initAppLoader(AnnotationProcessorContext context) {
        if (jpaAppLoader != null) {
            return;
        }
        String generatedClassName = context.generatedPackage() + ".JPAAppLoader__" + context.nextId();
        jpaAppLoader = new ClassBuilder()
                .setClassName(generatedClassName)
                .addClassAnnotation("@Singleton")
                .addImportClass(Singleton.class)
                .addImportClass(Named.class)
                .addImportClass(Factory.class)
                .addImportClass(EntityManager.class)
                .addImportClass(EntityManagerFactory.class)
                .addImportClass(Persistence.class)
                .addImportClass(SynchronizationType.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Map.class)
                .addImportClass(LinkedHashMap.class)
                .addImportClass("org.slf4j.Logger")
                .addImportClass("org.slf4j.LoggerFactory");
    }
}
