package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.http.server.HttpServerConfiguration;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.RxFilter;
import vest.doctor.http.server.RxHandler;
import vest.doctor.http.server.RxRequest;
import vest.doctor.http.server.RxResponse;
import vest.doctor.pipeline.Pipeline;
import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static vest.doctor.http.server.rest.ANY.ANY_METHOD_NAME;

/**
 * A handler that uses path specifications to route to handlers internally.
 * <p>
 * A path specification is a regular expression matching construct similar to JAX-RS routing.
 * In it's simplest form, a path specification can be a literal path: /api/v2/data.
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
 * Paths are matched first against the path override attribute from request.attribute({@link RxRouter#PATH_OVERRIDE}),
 * if it exists, otherwise they are matched against {@link Request#path()} which is the full path requested
 * in the HTTP request.
 * <p>
 * Path specifications can use the wildcard '*' to indicate that any path structure is accepted, e.g.
 * <code>/api/v2/*</code>. In this case, routes will have access to the '*' path parameter, containing
 * the entire path that was matched by the wildcard. So, for example, a request to /api/v2/that/thing
 * the '*' path parameter would be "that/thing".
 */
public final class RxRouter implements RxHandler {

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

    private static final RxHandler NOT_FOUND = request ->
            Pipeline.of(request.createResponse()
                    .status(404)
                    .body(ResponseBody.of("unknown route: " + request.method() + " " + request.path())));

    private final List<Tuple2<PathSpec, RxFilter>> filters = new LinkedList<>();
    private final Map<HttpMethod, List<Tuple2<PathSpec, RxHandler>>> routes = new TreeMap<>();
    private final HttpServerConfiguration conf;

    /**
     * Create a new Router, equivalent to <code>new Router(true)</code>
     */
    public RxRouter(HttpServerConfiguration conf) {
        this.conf = conf;
    }

    /**
     * Add new handler to this router.
     *
     * @param method  the HTTP method that the request must match to trigger the given handler
     * @param path    the path specification for the route
     * @param handler the handler that will be routed for the given method and path
     * @return this router
     */
    public RxRouter route(String method, String path, RxHandler handler) {
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
    public RxRouter route(HttpMethod method, String path, RxHandler handler) {
        if (method.equals(HttpMethod.GET)) {
            // cross list all GETs as HEADs
            route(HttpMethod.HEAD, path, handler);
        }
        PathSpec newSpec = new PathSpec(path, conf.getCaseInsensitiveMatching());
        List<Tuple2<PathSpec, RxHandler>> routes = this.routes.computeIfAbsent(method, v -> new ArrayList<>());
        if (routes.stream().anyMatch(r -> r.first().getPattern().toString().equals(newSpec.getPattern().toString()))) {
            throw new IllegalArgumentException("attempted to register duplicate path for " + method + " " + path);
        }
        routes.add(Tuple.of(newSpec, handler));
        routes.sort(Comparator.comparing(Tuple2::first));
        return this;
    }

    /**
     * Add a new filter to this router that will trigger for all requests.
     * Equivalent to <code>router.filter("/*", filter)</code>
     *
     * @param filter the filter to add
     * @return this router
     */
    public RxRouter filter(RxFilter filter) {
        return filter(MATCH_ALL_PATH_SPEC, filter);
    }

    /**
     * Add a new filter to this router.
     *
     * @param path   the path specification for the filter
     * @param filter the filter that will be triggered for the given path
     * @return this router
     */
    public RxRouter filter(String path, RxFilter filter) {
        filters.add(Tuple.of(new PathSpec(path, conf.getCaseInsensitiveMatching()), filter));
        filters.sort(Comparator.comparingInt(t -> t.second().priority()));
        return this;
    }

    @Override
    public Pipeline<RxResponse> handle(RxRequest request) {
        if (conf.isDebugRequestRouting()) {
            request.attribute(DEBUG_START_ATTRIBUTE, System.nanoTime());
        }
        Pipeline<RxResponse> pipeline = selectHandler(request)
                .handle(request)
                .defaultExecutor(request.pool());

        for (Tuple2<PathSpec, RxFilter> filter : filters) {
            Map<String, String> pathParams = filter.first().matchAndCollect(request.path());
            addTraceMessage(request, filter.first(), filter.second(), pathParams != null);
            if (pathParams != null) {
                pipeline = filter.second().filter(request, pipeline);
            }
        }

        return pipeline.observe(rxResponse -> attachTracing(rxResponse.request(), rxResponse));
    }

    protected RxHandler selectHandler(RxRequest request) {
        HttpMethod httpMethod = attributeOrElse(request, METHOD_OVERRIDE, request.method());
        RxHandler handler = selectHandlerWithMethod(request, httpMethod);
        if (handler != null) {
            return handler;
        }
        handler = selectHandlerWithMethod(request, ANY);
        if (handler != null) {
            return handler;
        }
        // not found
        addTraceMessage(request, "no matching route found");
        return NOT_FOUND;
    }

    private RxHandler selectHandlerWithMethod(RxRequest request, HttpMethod method) {
        for (Tuple2<PathSpec, RxHandler> route : routes.getOrDefault(method, Collections.emptyList())) {
            String path = attributeOrElse(request, PATH_OVERRIDE, request.path());
            Map<String, String> pathParams = route.first().matchAndCollect(path);
            addTraceMessage(request, route.first(), route.second(), method.name(), pathParams != null);
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                return route.second();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:\n");
        if (!filters.isEmpty()) {
            sb.append(" Filters:\n");
            for (Tuple2<PathSpec, RxFilter> filterAndPath : filters) {
                sb.append("  ")
                        .append(filterAndPath.first()).append(' ')
                        .append(filterAndPath.second()).append('\n');
            }
        }
        sb.append(" Routes:\n");
        for (Map.Entry<HttpMethod, List<Tuple2<PathSpec, RxHandler>>> entry : routes.entrySet()) {
            if (entry.getKey().equals(HttpMethod.HEAD)) {
                continue;
            }
            sb.append("  ").append(entry.getKey()).append('\n');
            for (Tuple2<PathSpec, RxHandler> route : entry.getValue()) {
                sb.append("   ").append(route.first())
                        .append(' ')
                        .append(route.second()).append('\n');
            }
        }
        return sb.toString();
    }

    private static <T> T attributeOrElse(RxRequest request, String attribute, T orElse) {
        T val = request.attribute(attribute);
        return val != null ? val : orElse;
    }

    private void attachTracing(RxRequest request, RxResponse response) {
        if (conf.isDebugRequestRouting()) {
            List<String> trace = request.attribute(DEBUG_ROUTING_ATTRIBUTE);
            if (trace != null) {
                for (String info : trace) {
                    response.headers().add("X-Route-Tracing", info);
                }
            }
        }
    }

    private void addTraceMessage(RxRequest request, PathSpec pathSpec, RxFilter stage, boolean matched) {
        if (conf.isDebugRequestRouting()) {
            addTraceMessage(request, (matched ? "match" : "no-match") +
                    " filter " +
                    pathSpec + ' ' +
                    stage);
        }
    }

    private void addTraceMessage(RxRequest request, PathSpec pathSpec, RxHandler handler, String method, boolean matched) {
        if (conf.isDebugRequestRouting()) {
            addTraceMessage(request, (matched ? "match" : "no-match") +
                    " route " + method + " " +
                    pathSpec + ' ' +
                    handler);
        }
    }

    private void addTraceMessage(RxRequest request, String info) {
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
