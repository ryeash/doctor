package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;
import java.util.regex.Pattern;

public class RouteConfig implements Comparable<RouteConfig> {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{.*?}");
    private static final Pattern SPLAT_PARAM_PATTERN = Pattern.compile("/\\*");
    private static final String DEFAULT_PARAM_REGEX = "[^/]+?";

    private PathSpec pathSpec;
    private final HttpMethod method;
    private final Route route;

    public RouteConfig(HttpMethod method, String path, Route route) {
        this.pathSpec = new PathSpec(path);
        this.method = method;
        this.route = route;
    }

    public HttpMethod method() {
        return method;
    }

    public Route route() {
        return route;
    }

    @Override
    public String toString() {
        return method + " " + pathSpec;
    }

    @Override
    public int compareTo(RouteConfig o) {
        // priority is the first check
        if (o.route.priority() != route.priority()) {
            return Integer.compare(route.priority(), o.route.priority());
        }
        return pathSpec.compareTo(o.pathSpec);
    }

    public String path() {
        return pathSpec.getPath();
    }

    public Map<String, String> matchAndCollect(String requestUri) {
        return pathSpec.matchAndCollect(requestUri);
    }
}