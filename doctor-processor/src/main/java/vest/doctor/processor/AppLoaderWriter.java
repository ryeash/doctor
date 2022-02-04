package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.ApplicationLoader;
import vest.doctor.DoctorProvider;
import vest.doctor.PrimaryProviderWrapper;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.processing.AnnotationProcessorContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class AppLoaderWriter {

    private final AnnotationProcessorContext context;
    private final ClassBuilder appLoader;
    private final MethodBuilder stage3;
    private boolean changed;

    public AppLoaderWriter(AnnotationProcessorContext context) {
        this.context = context;
        String className = context.generatedPackage() + ".AppLoaderImpl_" + context.nextId();
        this.appLoader = new ClassBuilder()
                .setClassName(className)
                .addImplementsInterface(ApplicationLoader.class)
                .addImportClass(List.class)
                .addImportClass(LinkedList.class)
                .addImportClass(Objects.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(Provider.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(PrimaryProviderWrapper.class)
                .addClassAnnotation("@SuppressWarnings(\"unchecked\")")
                .addField("private final List<", DoctorProvider.class, "<?>> eagerList = new LinkedList<>()");
        this.stage3 = appLoader.newMethod("public void stage3(", ProviderRegistry.class, " {{providerRegistry}})");
        MethodBuilder stage5 = appLoader.newMethod("public void stage5(", ProviderRegistry.class, " {{providerRegistry}})");
        stage5.line("eagerList.stream().filter(Objects::nonNull).forEach(", Provider.class, "::get);");
        changed = false;
    }

    public ClassBuilder classBuilder() {
        changed = true;
        return appLoader;
    }

    public MethodBuilder stage3() {
        changed = true;
        return stage3;
    }

    public void finish() {
        if (changed) {
            appLoader.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, appLoader.getFullyQualifiedClassName());
        }
    }
}
