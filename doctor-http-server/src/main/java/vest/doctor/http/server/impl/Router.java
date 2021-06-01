package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.Prioritized;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static vest.doctor.http.server.rest.ANY.ANY_METHOD_NAME;

public final class Router implements Handler {
    public static final String PATH_OVERRIDE = "doctor.netty.router.pathOverride";
    public static final String METHOD_OVERRIDE = "doctor.netty.router.methodOverride";
    public static final String PATH_PARAMS = "doctor.netty.router.pathparams";
    public static final HttpMethod ANY = HttpMethod.valueOf(ANY_METHOD_NAME);
    public static final Handler NOT_FOUND = new NotFound();

    private final List<FilterAndPath> filters = new LinkedList<>();
    private final Map<HttpMethod, List<Route>> routes = new TreeMap<>();
    private final boolean caseInsensitiveMatch;

    public Router() {
        this(true);
    }

    public Router(boolean caseInsensitiveMatch) {
        this.caseInsensitiveMatch = caseInsensitiveMatch;
    }

    public Router route(String method, String path, Handler handler) {
        return route(HttpMethod.valueOf(method), path, handler);
    }

    public Router route(HttpMethod method, String path, Handler handler) {
        if (method.equals(HttpMethod.GET)) {
            // cross list all GETs as HEADs
            route(HttpMethod.HEAD, path, handler);
        }
        Route newRoute = new Route(path, caseInsensitiveMatch, handler);

        List<Route> routes = this.routes.computeIfAbsent(method, v -> new ArrayList<>());
        if (routes.stream().anyMatch(r -> r.getPathSpec().getPattern().toString().equals(newRoute.getPathSpec().getPattern().toString()))) {
            throw new IllegalArgumentException("attempted to register duplicate path for " + method + " " + path);
        }
        routes.add(newRoute);
        routes.sort(Comparator.comparing(Route::getPathSpec));
        return this;
    }

    public Router filter(Filter filter) {
        return filter("/*", filter);
    }

    public Router filter(String path, Filter filter) {
        filters.add(new FilterAndPath(new PathSpec(path, caseInsensitiveMatch), filter));
        filters.sort(Prioritized.COMPARATOR);
        return this;
    }

    @Override
    public CompletionStage<Response> handle(Request request) throws Exception {
        CompletableFuture<Response> parent = new CompletableFuture<>();
        CompletionStage<Response> temp = parent;

        for (FilterAndPath filterAndPath : filters) {
            PathSpec pathSpec = filterAndPath.pathSpec;
            Filter filter = filterAndPath.filter;

            String path = attributeOrElse(request, PATH_OVERRIDE, request.path());
            Map<String, String> pathParams = pathSpec.matchAndCollect(path);
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                temp = filter.filter(request, temp);
                if (temp.toCompletableFuture().isDone()) {
                    return temp;
                }
            }
        }

        CompletionStage<Response> response = selectHandler(request).handle(request);
        forward(response, parent);
        return temp;
    }

    protected Handler selectHandler(Request request) {
        HttpMethod httpMethod = attributeOrElse(request, METHOD_OVERRIDE, request.method());
        Handler handler = selectHandler(request, httpMethod);
        if (handler != null) {
            return handler;
        }
        handler = selectHandler(request, ANY);
        if (handler != null) {
            return handler;
        }
        // not found
        return NOT_FOUND;
    }

    private Handler selectHandler(Request request, HttpMethod method) {
        for (Route route : routes.getOrDefault(method, Collections.emptyList())) {
            String path = attributeOrElse(request, PATH_OVERRIDE, request.path());
            Map<String, String> pathParams = route.getPathSpec().matchAndCollect(path);
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                return route.getHandler();
            }
        }
        return null;
    }

    private static <T> void forward(CompletionStage<T> source, CompletableFuture<T> receiver) {
        source.whenComplete((t, error) -> {
            if (error != null) {
                receiver.completeExceptionally(error);
            } else {
                receiver.complete(t);
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:\n");
        if (!filters.isEmpty()) {
            sb.append(" Filters:\n");
            for (FilterAndPath filterAndPath : filters) {
                sb.append("  ")
                        .append(filterAndPath.pathSpec).append(' ')
                        .append(filterAndPath.filter).append('\n');
            }
        }
        sb.append(" Routes:\n");
        for (Map.Entry<HttpMethod, List<Route>> entry : routes.entrySet()) {
            if (entry.getKey().equals(HttpMethod.HEAD)) {
                continue;
            }
            sb.append("  ").append(entry.getKey()).append('\n');
            for (Route route : entry.getValue()) {
                sb.append("   ").append(route.getPathSpec())
                        .append(' ')
                        .append(route.getHandler()).append('\n');
            }
        }
        return sb.toString();
    }

    private static <T> T attributeOrElse(Request request, String attribute, T orElse) {
        T val = request.attribute(attribute);
        return val != null ? val : orElse;
    }

    private static final class FilterAndPath implements Prioritized {
        private final PathSpec pathSpec;
        private final Filter filter;

        private FilterAndPath(PathSpec pathSpec, Filter filter) {
            this.pathSpec = pathSpec;
            this.filter = filter;
        }

        @Override
        public int priority() {
            return filter.priority();
        }
    }
}
