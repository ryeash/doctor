package vest.doctor.http.server.impl;

import vest.doctor.Prioritized;
import vest.doctor.http.server.Filter;

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
