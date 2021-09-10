package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.Prioritized;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpServerConfiguration;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static vest.doctor.http.server.rest.ANY.ANY_METHOD_NAME;

/**
 * A handler that uses path specifications to route to handlers internally.
 * <p>
 * A path specification is a regular expression matching construct similar to JAX-RS routing.
 * In its simplest form, a path specification can be a literal path: /api/v2/data.
 * Extending it to have path parameters: <code>/api/v2/data/{id}</code>. In this case the handler
 * registered with this path specification will have access to the "id" path parameter via
 * <code>Map<String, String> map = request.attribute(Router.PATH_PARAMS);</code>
 * <p>
 * Internally, path specification are converted into regular expressions mapping path parameter
 * names to matching groups. You can define the regular expressions for the declared path parameters
 * using the syntax: <code>/api/v2/data/{id:[^/]+?}</code>. Everything after the ':' is the regular expression
 * for the parameter. These two paths are converted to the same internal regular expression:
 * <code>/api/v2/data/{id}</code>, <code>/api/v2/data/{id:[^/]+?}</code>.
 * <p>
 * Paths are matched first against the path override attribute from request.attribute({@link Router#PATH_OVERRIDE}),
 * if it exists, otherwise they are matched against {@link Request#path()} which is the full path requested
 * in the HTTP request.
 * <p>
 * Path specifications can use the wildcard '*' to indicate that any path structure is accepted, e.g.
 * <code>/api/v2/*</code>. In this case, routes will have access to the '*' path parameter, containing
 * the entire path that was matched by the wildcard. So, for example, a request to /api/v2/that/thing
 * the '*' path parameter would be "that/thing".
 */
public final class Router implements Handler {

    /**
     * The name of the path override attribute. Can be used to override the request path
     * in e.g. a filter via <code>request.attribute(Router.PATH_OVERRIDE, request.path().toLowerCase());</code>.
     */
    public static final String PATH_OVERRIDE = "doctor.netty.router.pathOverride";

    /**
     * The name of the method override attribute. Can be used to override the request method
     * in e.g. a filter via <code>request.attribute(Router.METHOD_OVERRIDE, "GET");</code>.
     */
    public static final String METHOD_OVERRIDE = "doctor.netty.router.methodOverride";

    /**
     * The name of the path parameters attribute. Used in routes to get the path parameters
     * based on the matched path specification.
     */
    public static final String PATH_PARAMS = "doctor.netty.router.pathparams";

    /**
     * A path specification string that will match any requested path.
     */
    public static final String MATCH_ALL_PATH_SPEC = "/*";

    /**
     * The ANY method. This method can be used to create a route that responds to any HTTP method.
     */
    public static final HttpMethod ANY = HttpMethod.valueOf(ANY_METHOD_NAME);

    private static final String DEBUG_ROUTING_ATTRIBUTE = "doctor.netty.router.debugInfo";
    private static final String DEBUG_START_ATTRIBUTE = "doctor.netty.router.debugStart";
    private static final String FILTER_ITERATOR = "doctor.netty.router.filterIterator";

    private static final Handler NOT_FOUND = new NotFound();
    private final List<FilterAndPath> filters = new LinkedList<>();
    private final Map<HttpMethod, List<Route>> routes = new TreeMap<>();
    private final HttpServerConfiguration conf;

    /**
     * Create a new Router, equivalent to <code>new Router(true)</code>
     */
    public Router(HttpServerConfiguration conf) {
        this.conf = conf;
    }

    /**
     * Get the {@link HttpServerConfiguration} that was used to configure the {@link vest.doctor.http.server.HttpServer}.
     */
    public HttpServerConfiguration configuration() {
        return conf;
    }

    /**
     * Add new handler to this router.
     *
     * @param method  the HTTP method that the request must match to trigger the given handler
     * @param path    the path specification for the route
     * @param handler the handler that will be routed for the given method and path
     * @return this router
     */
    public Router route(String method, String path, Handler handler) {
        return route(HttpMethod.valueOf(method), path, handler);
    }

    /**
     * Add a new handler to this router.
     *
     * @param method  the HTTP method that the request must match to trigger the given handler
     * @param path    that path specification for the route
     * @param handler the handler that will be routed for the given method and path
     * @return this router
     */
    public Router route(HttpMethod method, String path, Handler handler) {
        String fullPath = conf.getRouterPrefix() + path;
        if (method.equals(HttpMethod.GET)) {
            // cross list all GETs as HEADs
            route(HttpMethod.HEAD, fullPath, handler);
        }
        Route newRoute = new Route(fullPath, conf.getCaseInsensitiveMatching(), handler);

        List<Route> routes = this.routes.computeIfAbsent(method, v -> new ArrayList<>());
        if (routes.stream().anyMatch(r -> r.getPathSpec().getPattern().toString().equals(newRoute.getPathSpec().getPattern().toString()))) {
            throw new IllegalArgumentException("attempted to register duplicate path for " + method + " " + path);
        }
        routes.add(newRoute);
        routes.sort(Comparator.comparing(Route::getPathSpec));
        return this;
    }

    /**
     * Add a new filter to this router that will trigger for all requests.
     * Equivalent to <code>router.filter("/*", filter)</code>
     *
     * @param filter the filter to add
     * @return this router
     */
    public Router filter(Filter filter) {
        return filter(MATCH_ALL_PATH_SPEC, filter);
    }

    /**
     * Add a new filter to this router.
     *
     * @param path   the path specification for the filter
     * @param filter the filter that will be triggered for the given path
     * @return this router
     */
    public Router filter(String path, Filter filter) {
        String fullPath = conf.getRouterPrefix() + path;
        filters.add(new FilterAndPath(new PathSpec(fullPath, conf.getCaseInsensitiveMatching()), filter));
        filters.sort(Prioritized.COMPARATOR);
        return this;
    }

    @Override
    public CompletionStage<Response> handle(Request request) throws Exception {
        if (conf.isDebugRequestRouting()) {
            request.attribute(DEBUG_START_ATTRIBUTE, System.nanoTime());
            addTraceMessage(request, "request " + request.method() + " " + request.path());
        }
        Iterator<FilterAndPath> iterator = filters.iterator();
        request.attribute(FILTER_ITERATOR, iterator);
        return doNextFilter(request);
    }

    private CompletionStage<Response> doNextFilter(Request request) throws Exception {
        Iterator<FilterAndPath> iterator = request.attribute(FILTER_ITERATOR);
        while (iterator.hasNext()) {
            FilterAndPath next = iterator.next();
            PathSpec pathSpec = next.pathSpec();
            Filter filter = next.filter();
            Map<String, String> pathParams = pathSpec.matchAndCollect(request);
            addTraceMessage(request, next, pathParams != null);
            if (pathParams != null) {
                request.attribute(Router.PATH_PARAMS, pathParams);
                return filter.filter(request, this::doNextFilter);
            }
        }
        return selectHandler(request).handle(request);
    }

    private Handler selectHandler(Request request) {
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
        addTraceMessage(request, "no matching route found");
        return NOT_FOUND;
    }

    private Handler selectHandler(Request request, HttpMethod method) {
        for (Route route : routes.getOrDefault(method, Collections.emptyList())) {
            Map<String, String> pathParams = route.getPathSpec().matchAndCollect(request);
            addTraceMessage(request, method.name(), route, pathParams != null);
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                return route.getHandler();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:\n");
        if (!filters.isEmpty()) {
            sb.append(" Filters:\n");
            for (FilterAndPath filterAndPath : filters) {
                sb.append("  ").append(filterAndPath).append('\n');
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

    static <T> T attributeOrElse(Request request, String attribute, T orElse) {
        T val = request.attribute(attribute);
        return val != null ? val : orElse;
    }

    void addTraceMessage(Request request, String routeMethod, Route route, boolean matched) {
        if (conf.isDebugRequestRouting()) {
            addTraceMessage(request, "route " +
                    (matched ? "match" : "not-matched") + ' ' +
                    routeMethod + ' ' +
                    route.getPathSpec() + ' ' +
                    route.getHandler());
        }
    }

    void addTraceMessage(Request request, FilterAndPath filter, boolean matched) {
        if (conf.isDebugRequestRouting()) {
            addTraceMessage(request, "filter " +
                    (matched ? "match" : "not-matched") + ' ' +
                    filter.pathSpec() + ' ' +
                    filter.filter());
        }
    }

    void addTraceMessage(Request request, String info) {
        List<String> trace = request.attribute(DEBUG_ROUTING_ATTRIBUTE);
        if (trace == null) {
            trace = new LinkedList<>();
            request.attribute(DEBUG_ROUTING_ATTRIBUTE, trace);
        }
        String dur = Optional.ofNullable(request.<Long>attribute(DEBUG_START_ATTRIBUTE))
                .map(start -> System.nanoTime() - start)
                .map(duration -> TimeUnit.MICROSECONDS.convert(duration, TimeUnit.NANOSECONDS) + "us")
                .orElse("");
        trace.add(dur + " " + info);
    }
}
