package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.ApplicationLoader;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.PrimaryProviderWrapper;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.runtime.AbstractApplicationLoader;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

public class AppLoaderWriter {

    private final AnnotationProcessorContext context;
    private final ClassBuilder appLoader;
    private final Map<Integer, MethodBuilder> stages = new ConcurrentSkipListMap<>();
    private boolean changed;

    public AppLoaderWriter(AnnotationProcessorContext context) {
        this.context = context;
        String className = context.generatedPackage() + ".AppLoaderImpl$" + context.nextId();
        this.appLoader = new ClassBuilder()
                .setClassName(className)
                .setExtendsClass(AbstractApplicationLoader.class)
                .addImportClass(List.class)
                .addImportClass(LinkedList.class)
                .addImportClass(Objects.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(PrimaryProviderWrapper.class)
                .addImportClass(Eager.class)
                .addImportClass("org.slf4j.Logger")
                .addImportClass("org.slf4j.LoggerFactory")
                .addClassAnnotation("@SuppressWarnings(\"unchecked\")");
        stage5().line("""
                {{providerRegistry}}.allProviders()
                .filter(p -> p.typeInfo().annotationMetadata().findOne(Eager.class).isPresent())
                .forEach(Provider::get);""");
        changed = false;
    }

    public ClassBuilder classBuilder() {
        changed = true;
        return appLoader;
    }

    public MethodBuilder stage1() {
        return stage(1);
    }

    public MethodBuilder stage2() {
        return stage(2);
    }

    public MethodBuilder stage3() {
        return stage(3);
    }

    public MethodBuilder stage4() {
        return stage(4);
    }

    public MethodBuilder stage5() {
        return stage(5);
    }

    private MethodBuilder stage(int stageNumber) {
        changed = true;
        return stages.computeIfAbsent(stageNumber, n -> classBuilder()
                .newMethod("@Override public void stage", n, "(", ProviderRegistry.class, " {{providerRegistry}})"));
    }

    public void finish() {
        if (changed) {
            appLoader.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, appLoader.getFullyQualifiedClassName());
        }
    }
}
