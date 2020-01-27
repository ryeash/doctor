package vest.doctor.jpa;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.Factory;
import vest.doctor.MethodBuilder;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;

import javax.inject.Named;
import javax.inject.Singleton;
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
        String pcName = Objects.requireNonNull(persistenceContext.name(), "@PersistenceContext annotations must have a name defined (matches the persistence unit name in the xml)");

        if (processedPersistenceUnits.contains(pcName)) {
            context.warnMessage("multiple @PersistenceContext annotations with the same name: " + pcName);
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
                .addImportClass(EntityManager.class)
                .addImportClass(EntityManagerFactory.class)
                .addImportClass(Persistence.class)
                .addImportClass(SynchronizationType.class)
                .addImportClass(BeanProvider.class)
                .addImportClass(Map.class)
                .addImportClass(LinkedHashMap.class)
                .addImportClass("org.slf4j.Logger")
                .addImportClass("org.slf4j.LoggerFactory");

        entityManagerFactory.addField("private final static Logger log = LoggerFactory.getLogger(" + generatedClassName + ".class)");

        MethodBuilder mb = new MethodBuilder("@Singleton @Factory @Named(\"" + pcName + "\") " +
                "public " + EntityManager.class.getSimpleName() + " entityManagerFactory" + context.nextId() + "(BeanProvider beanProvider)");
        mb.line("Map<String, String> properties = new LinkedHashMap<>();");
        for (PersistenceProperty property : persistenceContext.properties()) {
            mb.line("properties.put(beanProvider.resolvePlaceholders(\"" + property.name() + "\"), beanProvider.resolvePlaceholders(\"" + property.value() + "\"));");
        }
        mb.line("EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(beanProvider.resolvePlaceholders(\"" + pcName + "\"), properties);");
        mb.line("try{");
        mb.line("return entityManagerFactory.createEntityManager(SynchronizationType." + persistenceContext.synchronization() + ", properties);");
        mb.line("} catch (" + IllegalStateException.class.getSimpleName() + " e) {");
        mb.line("log.warn(\"could not create entity manager with explicit synchronization type, falling back; error message: {}\", e.getMessage());");
        mb.line("log.debug(\"full error stack\", e);");
        mb.line("return entityManagerFactory.createEntityManager(properties);");
        mb.line("}");
        entityManagerFactory.addMethod(mb.finish());
        entityManagerFactory.writeClass(context.filer());
    }
}
