package vest.doctor.http.server.impl;

import vest.doctor.Prioritized;
import vest.doctor.http.server.Filter;
import vest.doctor.netty.common.PathSpec;

record FilterAndPath(PathSpec pathSpec, Filter filter) implements Prioritized {
    @Override
    public int priority() {
        return filter.priority();
    }

    @Override
    public String toString() {
        return pathSpec + " " + filter;
    }
}
