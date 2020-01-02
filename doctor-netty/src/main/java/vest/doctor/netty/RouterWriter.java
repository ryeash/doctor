package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.DoctorProvider;
import vest.doctor.MethodBuilder;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDependency;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RouterWriter implements ProviderCustomizationPoint {

    private final ClassBuilder routerBuilder = new ClassBuilder()
            .addImplementsInterface(Route.class)
            .addImportClass(BeanProvider.class)
            .addImportClass(PathSpec.class)
            .addImportClass(Map.class)
            .addImportClass(DoctorProvider.class)
            .addImportClass(RequestContext.class);
    private final MethodBuilder init = new MethodBuilder("public void init(BeanProvider beanProvider)");
    private final MethodBuilder accept = new MethodBuilder("public void accept(RequestContext ctx)");

    private final Set<ExecutableElement> processedMethods = new HashSet<>();
    private final List<Meta> metadata = new LinkedList<>();

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) != null) {
            String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                    .map(Path::value)
                    .orElse(new String[]{"/"});

            for (ExecutableElement method : providerDefinition.methods()) {
                // TODO: filter only public methods
                if (processedMethods.add(method)) {
                    List<String> methods = getMethods(context, method);
                    if (!methods.isEmpty()) {

                        String[] paths = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(new String[]{""});
                        List<String> fullPaths = paths(roots, paths);

                        for (String httMethod : methods) {
                            for (String fullPath : fullPaths) {
                                Meta meta = new Meta();
                                meta.providerDefinition = providerDefinition;
                                meta.method = method;
                                meta.httpMethod = httMethod;
                                meta.path = new PathSpec(fullPath);
                                metadata.add(meta);
                            }
                        }
                    }
                }
            }
        }
        // unchanged
        return providerRef;
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        metadata.sort(Comparator.comparing(m -> m.path));

        accept.line("Map<String, String> pathParams = null;");
        int i = 0;
        for (Meta metadatum : metadata) {
            String specField = "spec" + (i++);
            routerBuilder.addField("private PathSpec " + specField + " = new PathSpec(\"" + metadatum.path.getPath() + "\");");
            routerBuilder.addField("private DoctorProvider<" + metadatum.providerDefinition.providedType().getQualifiedName() + "> " + metadatum.providerDefinition.uniqueInstanceName());

            ProviderDependency providerDependency = metadatum.providerDefinition.asDependency();
            init.line(metadatum.providerDefinition.uniqueInstanceName() + " = beanProvider.getProvider(" + providerDependency.type().getQualifiedName() + ".class, " + providerDependency.qualifier() + ");");

            accept.line("pathParams = " + specField + ".matchAndCollect(ctx.requestUri().getRawPath());");
            accept.line("if(pathParams != null){")
                    .line(metadatum.buildMethodCall(metadatum.providerDefinition.uniqueInstanceName(), "ctx") + ";")
                    .line("return;")
                    .line("}");
        }

        accept.line("ctx.response(404, \"\");")
                .line("ctx.complete();");

        routerBuilder.addMethod(init.finish());
        routerBuilder.addMethod(accept.finish());
        routerBuilder.setClassName(context.generatedPackage() + ".RouterImpl");
        routerBuilder.writeClass(context.filer());

        try {
            FileObject sourceFile = context.filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/vest.doctor.netty.Route");
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(context.generatedPackage() + ".RouterImpl");
            }
        } catch (IOException e) {
            context.errorMessage("error writing services resources");
            e.printStackTrace();
        }
    }

    private List<String> getMethods(AnnotationProcessorContext context, ExecutableElement method) {
        List<String> methods = new LinkedList<>();
        for (AnnotationMirror am : context.processingEnvironment().getElementUtils().getAllAnnotationMirrors(method)) {
            for (AnnotationMirror annotationMirror : am.getAnnotationType().asElement().getAnnotationMirrors()) {
                if (annotationMirror.getAnnotationType().toString().equals(HttpMethod.class.getCanonicalName())) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
                        if (entry.getKey().getSimpleName().toString().equals("value")) {
                            methods.add(entry.getValue().getValue().toString());
                        }
                    }
                }
            }
        }
        return methods;
    }

    private List<String> paths(String[] roots, String[] paths) {
        List<String> allPaths = new LinkedList<>();
        for (String root : roots) {
            for (String path : paths) {
                allPaths.add(Utils.squeeze("/" + root + "/" + path, '/'));
            }
        }
        return allPaths;
    }

    private static final class Meta {
        ProviderDefinition providerDefinition;
        ExecutableElement method;
        String httpMethod;
        PathSpec path;

        public String buildMethodCall(String providerRef, String requestContextRef) {
            return providerRef + ".get()." + method.getSimpleName() + "(" + requestContextRef + ")";
        }
    }
}
