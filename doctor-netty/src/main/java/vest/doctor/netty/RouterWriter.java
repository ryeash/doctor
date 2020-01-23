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
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RouterWriter implements ProviderCustomizationPoint {

    private final ClassBuilder routerBuilder = new ClassBuilder()
            .addImplementsInterface(Router.class)
            .addImportClass(BeanProvider.class)
            .addImportClass(PathSpec.class)
            .addImportClass(Map.class)
            .addImportClass(DoctorProvider.class)
            .addImportClass(io.netty.handler.codec.http.HttpMethod.class)
            .addImportClass(BodyInterchange.class)
            .addImportClass(FilterStage.class)
            .addImportClass(HttpException.class)
            .addImportClass(RequestContext.class)
            .addImportClass(Websocket.class)
            .addImportClass(HashMap.class);

    private final MethodBuilder init = new MethodBuilder("public void init(BeanProvider beanProvider)");
    private final MethodBuilder accept = new MethodBuilder("public boolean accept(RequestContext ctx) throws Exception");
    private final MethodBuilder filter = new MethodBuilder("public void filter(FilterStage filterStage, RequestContext ctx)");

    private final Set<ExecutableElement> processedMethods = new HashSet<>();
    private final List<Meta> routeMetadata = new LinkedList<>();
    private final List<Meta> filterMetadata = new LinkedList<>();

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String beanProviderRef) {
        if (providerDefinition.annotationSource().getAnnotation(Path.class) != null) {
            String[] roots = Optional.ofNullable(providerDefinition.annotationSource().getAnnotation(Path.class))
                    .map(Path::value)
                    .orElse(new String[]{"/"});

            providerDefinition.methods()
                    .stream()
                    .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                    .filter(processedMethods::add)
                    .forEach(method -> {
                        List<String> methods = getHttpMethods(context, method);
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
                                    routeMetadata.add(meta);
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
                                filterMetadata.add(meta);
                            }
                        }
                    });

            if (providerDefinition.isCompatibleWith(Websocket.class)) {
                for (String path : roots) {
                    ProviderDependency providerDependency = providerDefinition.asDependency();
                    init.line("websockets.put(\"" + Utils.squeeze("/" + path, '/') + "\", beanProvider.getProvider(" + providerDependency.type().getQualifiedName() + ".class, " + providerDependency.qualifier() + ").get());");
                }
            }
        }
        // unchanged
        return providerRef;
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        routeMetadata.sort(Comparator.comparing(m -> m.path));

        accept.line("Map<String, String> pathParams = null;");
        accept.line("filter(FilterStage." + FilterStage.BEFORE_MATCH + ", ctx);");
        accept.line("if(ctx.isHalted()) { return true; }");
        int i = 0;

        Set<String> usedProviders = new HashSet<>();

        routerBuilder.addField("private static final HttpMethod HEAD = HttpMethod.HEAD");
        routerBuilder.addField("private BodyInterchange bodyInterchange");
        init.line("bodyInterchange = new BodyInterchange(beanProvider);");

        Map<String, List<Meta>> methodToMetadata = routeMetadata.stream().collect(Collectors.groupingBy(m -> m.httpMethod, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<Meta>> entry : methodToMetadata.entrySet()) {
            String method = entry.getKey();

            routerBuilder.addField("private static final HttpMethod " + method + " = HttpMethod.valueOf(\"" + method + "\")");

            // special logic for 'HEAD' requests
            if (method.equals(io.netty.handler.codec.http.HttpMethod.GET.name())) {
                accept.line("if(ctx.requestMethod().equals(" + method + ") || ctx.requestMethod().equals(HEAD)){");
            } else {
                accept.line("if(ctx.requestMethod().equals(" + method + ")){");
            }

            for (Meta metadatum : entry.getValue()) {
                String specField = "spec" + (i++);
                routerBuilder.addField("private final PathSpec " + specField + " = new PathSpec(\"" + metadatum.path.method().name() + "\", \"" + metadatum.path.getPath() + "\")");

                if (usedProviders.add(metadatum.providerDefinition.uniqueInstanceName())) {
                    routerBuilder.addField("private DoctorProvider<" + metadatum.providerDefinition.providedType().getQualifiedName() + "> " + metadatum.providerDefinition.uniqueInstanceName());
                    ProviderDependency providerDependency = metadatum.providerDefinition.asDependency();
                    init.line(metadatum.providerDefinition.uniqueInstanceName() + " = beanProvider.getProvider(" + providerDependency.type().getQualifiedName() + ".class, " + providerDependency.qualifier() + ");");
                }

                accept.line("pathParams = " + specField + ".matchAndCollect(ctx.requestUri().getRawPath());");
                accept.line("if(pathParams != null){")
                        .line(metadatum.buildMethodCall(context, metadatum.providerDefinition.uniqueInstanceName(), "ctx") + ";")
                        .line("ctx.future().thenRun(() -> filter(FilterStage.AFTER_ROUTE, ctx));");

                if (metadatum.method.getReturnType().getKind() != TypeKind.VOID) {
                    accept.line("bodyInterchange.write(ctx, response);");
                } else {
                    accept.line("ctx.complete();");
                }
                accept.line("return true;")
                        .line("}");
            }
            accept.line("}");
        }

        accept.line("return false;");

        filter.line("try {");
        filter.line("Map<String, String> pathParams = null;");
        Map<FilterStage, List<Meta>> filterStageToMetadata = filterMetadata.stream().collect(Collectors.groupingBy(m -> m.method.getAnnotation(Filter.class).value()));
        for (Map.Entry<FilterStage, List<Meta>> entry : filterStageToMetadata.entrySet()) {
            filter.line("if(filterStage == FilterStage." + entry.getKey() + ") {");
            for (Meta metadatum : entry.getValue()) {
                String specField = "filterSpec" + (i++);
                routerBuilder.addField("private final PathSpec " + specField + " = new PathSpec(\"" + metadatum.path.method().name() + "\", \"" + metadatum.path.getPath() + "\")");

                if (usedProviders.add(metadatum.providerDefinition.uniqueInstanceName())) {
                    routerBuilder.addField("private DoctorProvider<" + metadatum.providerDefinition.providedType().getQualifiedName() + "> " + metadatum.providerDefinition.uniqueInstanceName());
                    ProviderDependency providerDependency = metadatum.providerDefinition.asDependency();
                    init.line(metadatum.providerDefinition.uniqueInstanceName() + " = beanProvider.getProvider(" + providerDependency.type().getQualifiedName() + ".class, " + providerDependency.qualifier() + ");");
                }

                filter.line("if(ctx.isHalted()) { return; }")
                        .line("pathParams = " + specField + ".matchAndCollect(ctx.requestUri().getRawPath());")
                        .line("if(pathParams != null){")
                        .line(metadatum.buildMethodCall(context, metadatum.providerDefinition.uniqueInstanceName(), "ctx") + ";")
                        .line("}");
            }
            filter.line("}");
        }
        filter.line("} catch (Throwable t) {")
                .line("throw new HttpException(\"error running filters\", t);")
                .line("}");

        routerBuilder.addMethod(init.finish());
        routerBuilder.addMethod(accept.finish());
        routerBuilder.addMethod(filter.finish());
        routerBuilder.addField("private final Map<String, Websocket> websockets = new HashMap<>()")
                .addMethod("public Websocket getWebsocket(String uri)", mb -> {
                    mb.line("return websockets.get(uri);");
                });
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

    private static List<String> getHttpMethods(AnnotationProcessorContext context, ExecutableElement method) {
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
            String parameters = method.getParameters().stream()
                    .map(p -> ParameterSupport.parameterWriting(context, p, requestContextRef))
                    .collect(Collectors.joining(", "));
            StringBuilder sb = new StringBuilder(providerRef + ".get()." + method.getSimpleName() + "(" + parameters + ")");
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                sb.insert(0, method.getReturnType() + " response = ");
            }
            return sb.toString();
        }
    }
}
