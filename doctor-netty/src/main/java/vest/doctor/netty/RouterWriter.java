package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ClassBuilder;
import vest.doctor.DoctorProvider;
import vest.doctor.MethodBuilder;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderDependency;
import vest.doctor.ProviderRegistry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static vest.doctor.Constants.PROVIDER_REGISTRY;

/**
 * Code generator for the {@link Router} implementation.
 */
public class RouterWriter implements ProviderDefinitionListener {

    private final ClassBuilder routerBuilder = new ClassBuilder()
            .setExtendsClass(AbstractRouter.class)
            .addImportClass(ProviderRegistry.class)
            .addImportClass(Route.class)
            .addImportClass(AbstractRoute.class)
            .addImportClass(PathSpec.class)
            .addImportClass(TypeInfo.class)
            .addImportClass(Map.class)
            .addImportClass(DoctorProvider.class)
            .addImportClass(io.netty.handler.codec.http.HttpMethod.class)
            .addImportClass(BodyInterchange.class)
            .addImportClass(FilterStage.class)
            .addImportClass(HttpException.class)
            .addImportClass(RequestContext.class)
            .addImportClass(Websocket.class)
            .addImportClass(HashMap.class);

    private final MethodBuilder init = new MethodBuilder("public void init(" + ProviderRegistry.class.getSimpleName() + " " + PROVIDER_REGISTRY + ")");

    private final Set<ExecutableElement> processedMethods = new HashSet<>();
    private final Set<String> usedProviders = new HashSet<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) != null) {
            String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                    .map(Path::value)
                    .orElse(new String[]{"/"});

            providerDefinition.methods()
                    .stream()
                    .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                    .filter(processedMethods::add)
                    .forEach(method -> {
                        List<String> methods = getHttpMethods(method);
                        if (!methods.isEmpty()) {
                            String[] paths = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(new String[]{""});
                            List<String> fullPaths = paths(roots, paths);

                            for (String httMethod : methods) {
                                for (String fullPath : fullPaths) {
                                    Meta meta = new Meta();
                                    meta.providerDefinition = providerDefinition;
                                    meta.method = method;
                                    meta.httpMethod = httMethod;
                                    meta.path = new PathSpec(httMethod, fullPath);
                                    writeRoute(context, meta);
                                }
                            }
                        }
                        if (method.getAnnotation(Filter.class) != null) {
                            String[] paths = Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(new String[]{""});
                            List<String> fullPaths = paths(roots, paths);
                            for (String fullPath : fullPaths) {
                                Meta meta = new Meta();
                                meta.providerDefinition = providerDefinition;
                                meta.method = method;
                                meta.path = new PathSpec("FILTER", fullPath);
                                writeRoute(context, meta);
                            }
                        }
                    });

            if (providerDefinition.isCompatibleWith(Websocket.class)) {
                for (String path : roots) {
                    ProviderDependency providerDependency = providerDefinition.asDependency();
                    init.line("websockets.put(\"{}\", {}.getProvider({}.class, {}).get());",
                            Utils.squeeze("/" + path, '/'), PROVIDER_REGISTRY, providerDependency.type().getQualifiedName(), providerDependency.qualifier());
                }
            }
        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        init.line("bodyInterchange = new BodyInterchange({});", PROVIDER_REGISTRY);
        init.line("postInit();");

        routerBuilder.addMethod(init.finish());
        routerBuilder.setClassName(context.generatedPackage() + ".RouterImpl");
        routerBuilder.writeClass(context.filer());

        try {
            FileObject sourceFile = context.filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/vest.doctor.netty.Router");
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(context.generatedPackage() + ".RouterImpl");
            }
        } catch (IOException e) {
            context.errorMessage("error writing services resources");
            e.printStackTrace();
        }
    }

    private void writeRoute(AnnotationProcessorContext context, Meta meta) {
        if (usedProviders.add(meta.providerDefinition.uniqueInstanceName())) {
            routerBuilder.addField("private DoctorProvider<" + meta.providerDefinition.providedType().getQualifiedName() + "> " + meta.providerDefinition.uniqueInstanceName());
            ProviderDependency providerDependency = meta.providerDefinition.asDependency();
            init.line("{} = {}.getProvider({}.class, {});",
                    meta.providerDefinition.uniqueInstanceName(), PROVIDER_REGISTRY, providerDependency.type(), providerDependency.qualifier());
        }

        boolean isVoid = meta.method.getReturnType().getKind() == TypeKind.VOID;
        String typeString = isVoid ? Object.class.getSimpleName() : meta.method.getReturnType().toString();

        if (meta.method.getAnnotation(Filter.class) != null) {
            FilterStage stage = meta.method.getAnnotation(Filter.class).value();
            init.line("addFilter(FilterStage.{}, new AbstractRoute<{}>(\"{}\", \"{}\") {", stage.name(), typeString, stage.name(), meta.path.getPath());
        } else {
            init.line("addRoute(new AbstractRoute<{}>(\"{}\", \"{}\") {", typeString, meta.httpMethod, meta.path.getPath());
        }

        init.line("public {} executeRoute(RequestContext ctx, BodyInterchange bodyInterchange) throws Exception {", typeString);
        init.line(meta.buildMethodCall(context, meta.providerDefinition.uniqueInstanceName(), "ctx") + ";");
        if (!isVoid) {
            init.line("return response;");
        } else {
            init.line("return null;");
        }
        init.line("}");
        init.line("});");
    }

    private static List<String> getHttpMethods(ExecutableElement method) {
        List<String> methods = new LinkedList<>();
        for (AnnotationMirror am : method.getAnnotationMirrors()) {
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

    private static List<String> paths(String[] roots, String[] paths) {
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

        public String buildMethodCall(AnnotationProcessorContext context, String providerRef, String requestContextRef) {
            StringBuilder sb = new StringBuilder();
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                sb.append(method.getReturnType()).append(" response = ");
            }
            sb.append(providerRef).append(".get().").append(method.getSimpleName());
            String parameters = method.getParameters().stream()
                    .map(p -> ParameterSupport.parameterWriting(context, p, requestContextRef))
                    .collect(Collectors.joining(", ", "(", ")"));
            sb.append(parameters);
            return sb.toString();
        }
    }
}
