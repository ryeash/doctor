package vest.doctor.jpa;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContexts;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.SynchronizationType;
import vest.doctor.Configuration;
import vest.doctor.DestroyMethod;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.Primary;
import vest.doctor.Prototype;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.CodeSnippet;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JPAFactoryWriter implements ProviderDefinitionListener {

    enum JPAType {
        entityManagerFactory, entityManager
    }

    public static final String USE_SYNCHRONIZATION_CONFIG = "doctor.jpa.useSynchronizationType";
    public static final String SET_PRIMARY_CONFIG = "doctor.jpa.primary";
    public static final String ENTITY_MANAGER_FACTORY_SCOPE_CONFIG = "doctor.jpa." + JPAType.entityManagerFactory + ".scope";
    public static final String ENTITY_MANAGER_SCOPE_CONFIG = "doctor.jpa." + JPAType.entityManager + ".scope";

    private final Set<String> unitNames = new HashSet<>();

    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        List<PersistenceContext> annotations = new LinkedList<>();
        Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(PersistenceContext.class))
                .ifPresent(annotations::add);
        Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(PersistenceContexts.class))
                .map(PersistenceContexts::value)
                .stream()
                .flatMap(Arrays::stream)
                .forEach(annotations::add);
        if (!annotations.isEmpty()) {
            ClassBuilder cb = new ClassBuilder()
                    .setClassName(providerDefinition.providedType().getQualifiedName() + "$JPAConfig")
                    .addImportClass(Configuration.class)
                    .addImportClass(Eager.class)
                    .addImportClass(Named.class)
                    .addImportClass(Primary.class)
                    .addImportClass(DestroyMethod.class)
                    .addImportClass(Factory.class)
                    .addImportClass(Prototype.class)
                    .addImportClass(EntityManager.class)
                    .addImportClass(EntityManagerFactory.class)
                    .addImportClass(Persistence.class)
                    .addImportClass(SynchronizationType.class)
                    .addImportClass(ConfigurationFacade.class)
                    .addImportClass(LinkedHashMap.class)
                    .addImportClass(Map.class)
                    .addClassAnnotation("@Configuration");

            for (PersistenceContext persistenceContext : annotations) {
                createJPAFactory(cb, persistenceContext, providerDefinition);
            }
            cb.writeClass(context.filer());
        }
    }

    private void createJPAFactory(ClassBuilder cb,
                                  PersistenceContext persistenceContext,
                                  ProviderDefinition providerDefinition) {
        String unitName = persistenceContext.unitName().trim();
        if (unitName.isEmpty()) {
            throw new CodeProcessingException("unitName must be set in PersistenceContext", providerDefinition.annotationSource());
        }
        if (!unitNames.add(persistenceContext.unitName())) {
            throw new CodeProcessingException("duplicate unit names processed: " + persistenceContext.unitName() + " -- please check PersistenceContext annotation", providerDefinition.annotationSource());
        }

        String qualifierName = ProcessorUtils.escapeAndQuoteStringForCode(Optional.of(persistenceContext.name())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(unitName));

        Map<String, String> propertyMap = new LinkedHashMap<>();
        for (PersistenceProperty property : persistenceContext.properties()) {
            propertyMap.put(property.name(), property.value());
        }


        CodeSnippet propCode = new CodeSnippet();
        propCode.line("Map<String, String> map = new LinkedHashMap<>();");
        for (PersistenceProperty property : persistenceContext.properties()) {
            propCode.printfLine("map.put(config.resolvePlaceholders(%s), config.resolvePlaceholders(%s));",
                    ProcessorUtils.escapeAndQuoteStringForCode(property.name()), ProcessorUtils.escapeAndQuoteStringForCode(property.value()));
        }

        CodeSnippet methodAnnotations = new CodeSnippet();
        methodAnnotations.line("@Factory");
        methodAnnotations.line("@Named(" + qualifierName + ")");
        if (Boolean.parseBoolean(propertyMap.getOrDefault(SET_PRIMARY_CONFIG, "false"))) {
            methodAnnotations.line("@Primary");
        }
        methodAnnotations.line();

        String emfScope = propertyMap.getOrDefault(ENTITY_MANAGER_FACTORY_SCOPE_CONFIG, Singleton.class.getCanonicalName());
        MethodBuilder emf = cb.newMethod("@" + emfScope + " " + methodAnnotations +
                                         " public EntityManagerFactory entityManagerFactory_" + ProcessorUtils.escapeStringForCode(unitName) + "(ConfigurationFacade config)");
        emf.line(propCode.toString());
        emf.printfLine("return Persistence.createEntityManagerFactory(%s, map);", ProcessorUtils.escapeAndQuoteStringForCode(unitName));

        String emScope = propertyMap.getOrDefault(ENTITY_MANAGER_SCOPE_CONFIG, Prototype.class.getCanonicalName());
        MethodBuilder em = cb.newMethod("@" + emScope + " " + methodAnnotations +
                                        " public EntityManager entityManager_" + ProcessorUtils.escapeStringForCode(unitName)
                                        + "(ConfigurationFacade config, @Named(" + qualifierName + ") EntityManagerFactory entityManagerFactory)");
        em.line(propCode.toString());
        if (Boolean.parseBoolean(propertyMap.getOrDefault(USE_SYNCHRONIZATION_CONFIG, "false"))) {
            SynchronizationType synchronization = persistenceContext.synchronization();
            em.printfLine("return entityManagerFactory.createEntityManager(SynchronizationType.%s, map);", synchronization);
        } else {
            em.line("return entityManagerFactory.createEntityManager(map);");
        }
    }
}
