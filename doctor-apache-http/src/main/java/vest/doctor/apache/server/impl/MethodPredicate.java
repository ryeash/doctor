package vest.doctor.apache.server.impl;

import vest.doctor.apache.server.RequestContext;
import vest.doctor.apache.server.RoutePredicate;

import java.util.Objects;

import static vest.doctor.apache.server.impl.Router.METHOD_OVERRIDE;

public class MethodPredicate implements RoutePredicate {
    private final String method;

    public MethodPredicate(String method) {
        this.method = Objects.requireNonNull(method);
    }

    @Override
    public boolean test(RequestContext requestContext) {
        String methodOverride = requestContext.getAttribute(METHOD_OVERRIDE);
        String testMethod = methodOverride != null ? methodOverride : requestContext.request().getMethod();
        return method.equalsIgnoreCase(testMethod);
    }

    @Override
    public String toString() {
        return method;
    }

    @Override
    public int compareTo(RoutePredicate o) {
        if (o instanceof MethodPredicate m) {
            return method.compareTo(m.method);
        } else {
            return RoutePredicate.super.compareTo(o);
        }
    }
}
