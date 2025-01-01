package vest.doctor.apache.server.impl;

import org.apache.hc.core5.http.HttpException;
import vest.doctor.apache.server.RequestContext;
import vest.doctor.apache.server.RoutePredicate;

import java.net.URISyntaxException;
import java.util.Map;

import static vest.doctor.apache.server.impl.Router.PATH_OVERRIDE;
import static vest.doctor.apache.server.impl.Router.PATH_PARAMS;

public class PathPredicate implements RoutePredicate {

    private final PathSpec pathSpec;

    public PathPredicate(String pathSpec, boolean caseInsensitive) {
        this.pathSpec = new PathSpec(pathSpec, caseInsensitive);
    }

    @Override
    public boolean test(RequestContext requestContext) {
        String pathOverride = requestContext.getAttribute(PATH_OVERRIDE);
        String path;
        try {
            path = pathOverride != null ? pathOverride : requestContext.request().getUri().getRawPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(new HttpException("error parsing uri", e));
        }
        Map<String, String> pathParams = pathSpec.matchAndCollect(path);
        if (pathParams != null) {
            requestContext.setAttribute(PATH_PARAMS, pathParams);
        }
        return pathParams != null;
    }

    @Override
    public String toString() {
        return pathSpec.toString();
    }

    @Override
    public int compareTo(RoutePredicate o) {
        if (o instanceof PathPredicate p) {
            return pathSpec.compareTo(p.pathSpec);
        } else {
            return RoutePredicate.super.compareTo(o);
        }
    }
}
