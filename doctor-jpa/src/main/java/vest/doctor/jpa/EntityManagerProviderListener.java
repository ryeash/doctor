package vest.doctor.jpa;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.SynchronizationType;
import vest.doctor.AdHocProvider;
import vest.doctor.DestroyMethod;
import vest.doctor.Factory;
import vest.doctor.InjectionException;
import vest.doctor.Prototype;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class EntityManagerProviderListener implements ProviderDefinitionListener {

    public static final String DOCTOR_JPA_EMF_INJECT_SCOPE = "doctor.jpa.entitymanagerfactory.inject.scope";
    public static final String DOCTOR_JPA_EM_INJECT_SCOPE = "doctor.jpa.entitymanager.inject.scope";
    public static final String DOCTOR_JPA_EM_USE_SYNC_TYPE = "doctor.jpa.entitymanager.useSynchronizationType";

    private final Set<String> processedPersistenceUnits = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        PersistenceContext[] persistenceContexts = providerDefinition.annotationSource().getAnnotationsByType(PersistenceContext.class);
        if (persistenceContexts != null && persistenceContexts.length > 0) {
            String packageName = context.generatedPackageName(providerDefinition.providedType());
            String jpaFactoryClass = packageName + ".JPAFactory_" + context.nextId();

            ClassBuilder jpaFactory = new ClassBuilder()
                    .setClassName(jpaFactoryClass)
                    .addImportClass(EntityManager.class)
                    .addImportClass(EntityManagerFactory.class)
                    .addImportClass(InjectionException.class)
                    .addImportClass(Persistence.class)
                    .addImportClass(SynchronizationType.class)
                    .addImportClass(ProviderRegistry.class)
                    .addImportClass(AdHocProvider.class)
                    .addImportClass(Map.class)
                    .addImportClass(Properties.class)
                    .addImportClass(LinkedHashMap.class)
                    .addImportClass(Factory.class)
                    .addImportClass(Named.class)
                    .addImportClass(DestroyMethod.class)
                    .addImportClass(Singleton.class)
                    .addImportClass("org.slf4j.Logger")
                    .addImportClass("org.slf4j.LoggerFactory")
                    .addClassAnnotation("@Singleton");

            for (PersistenceContext persistenceContext : persistenceContexts) {
                String unitName = Objects.requireNonNull(persistenceContext.unitName(), "@PersistenceContext annotations must have a unit name defined that matches the persistence unit name in the xml");
                String qualifierName = Optional.of(persistenceContext.name())
                        .filter(s -> !s.isEmpty())
                        .orElse(unitName);

                if (!processedPersistenceUnits.add(unitName)) {
                    throw new CodeProcessingException("multiple @PersistenceContext annotations with the same unit name: " + unitName);
                }

                String nameQualifierAnnotation = "@Named(" + ProcessorUtils.escapeAndQuoteStringForCode(qualifierName) + ")";
                MethodBuilder propertiesFactory = jpaFactory.newMethod("public Map<String, String> jpaProperties(ProviderRegistry providerRegistry)");
                propertiesFactory.addAnnotation(Factory.class);
                propertiesFactory.addAnnotation(Singleton.class);
                propertiesFactory.addAnnotation(nameQualifierAnnotation);
                propertiesFactory.line("Map<String, String> properties = new LinkedHashMap<>();");
                for (PersistenceProperty property : persistenceContext.properties()) {
                    propertiesFactory.line("properties.put({{providerRegistry}}.resolvePlaceholders(\"", property.name(), "\"), {{providerRegistry}}.resolvePlaceholders(\"", property.value(), "\"));");
                }
                propertiesFactory.line("return properties;");

                MethodBuilder entityManagerFactory = jpaFactory.newMethod("public EntityManagerFactory entityManagerFactory_", context.nextId(), "(ProviderRegistry providerRegistry,", nameQualifierAnnotation, " Map<String, String> properties)");
                entityManagerFactory.addAnnotation(Factory.class);
                entityManagerFactory.addAnnotation(scope(persistenceContext, DOCTOR_JPA_EMF_INJECT_SCOPE, Singleton.class));
                entityManagerFactory.addAnnotation(nameQualifierAnnotation);
                entityManagerFactory.addAnnotation("@DestroyMethod(\"close\")");
                entityManagerFactory.line("return Persistence.createEntityManagerFactory({{providerRegistry}}.resolvePlaceholders(\"", unitName, "\"), properties);");

                MethodBuilder entityManager = jpaFactory.newMethod("public EntityManager entityManager_", context.nextId(), "(", nameQualifierAnnotation, " EntityManagerFactory emf, ", nameQualifierAnnotation, " Map<String, String> properties)");
                entityManager.addAnnotation(Factory.class);
                entityManager.addAnnotation(scope(persistenceContext, DOCTOR_JPA_EM_INJECT_SCOPE, Prototype.class));
                entityManager.addAnnotation(nameQualifierAnnotation);
                entityManager.addAnnotation("@DestroyMethod(\"close\")");

                if (getUseSync(persistenceContext)) {
                    entityManager.line("try{");
                    entityManager.line("return emf.createEntityManager(SynchronizationType.", persistenceContext.synchronization(), ", properties);");
                    entityManager.line("} catch (", IllegalStateException.class.getSimpleName(), " e) {");
                    entityManager.line("throw new InjectionException(\"could not create entity manager with explicit synchronization type, try disabling @PersistenceProperty " + DOCTOR_JPA_EM_USE_SYNC_TYPE + "\", e);");
                    entityManager.line("}");
                } else {
                    entityManager.line("return emf.createEntityManager(properties);");
                }
            }
            jpaFactory.writeClass(context.filer());
        }
    }

    private static String scope(PersistenceContext pc, String propertyName, Class<? extends Annotation> defaultScope) {
        for (PersistenceProperty property : pc.properties()) {
            if (property.name().equals(propertyName)) {
                return "@" + property.value();
            }
        }
        return "@" + defaultScope.getCanonicalName();
    }

    private static boolean getUseSync(PersistenceContext pc) {
        for (PersistenceProperty property : pc.properties()) {
            if (property.name().equals(DOCTOR_JPA_EM_USE_SYNC_TYPE)) {
                return Boolean.parseBoolean(property.value());
            }
        }
        return false;
    }
}
