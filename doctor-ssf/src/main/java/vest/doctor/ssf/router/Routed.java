package vest.doctor.ssf.router;

import vest.doctor.ssf.Request;
import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.impl.Utils;

import java.util.Map;
import java.util.Objects;

public abstract class Routed implements Comparable<Routed> {
    protected final String method;
    protected final boolean isAnyMethod;
    protected final PathSpec pathSpec;

    protected Routed(String method, PathSpec pathSpec) {
        this.method = method;
        this.isAnyMethod = Objects.equals(method, Utils.ANY);
        this.pathSpec = pathSpec;
    }

    public boolean matches(RequestContext ctx) {
        Request request = ctx.request();
        if (isAnyMethod || Objects.equals(request.method(), this.method)) {
            Map<String, String> pathParams = pathSpec.matchAndCollect(request.uri().getRawPath());
            if (pathParams != null) {
                ctx.attribute(Router.PATH_PARAMS, pathParams);
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Routed o) {
        int c = this.method.compareTo(o.method);
        if (c != 0) {
            return c;
        } else {
            return this.pathSpec.compareTo(o.pathSpec);
        }
    }

    @Override
    public String toString() {
        return method + ' ' + pathSpec;
    }
}
