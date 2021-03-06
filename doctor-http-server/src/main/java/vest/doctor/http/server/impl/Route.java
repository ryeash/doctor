package vest.doctor.http.server.impl;

import vest.doctor.http.server.Handler;

final class Route {
    private final PathSpec pathSpec;
    private final Handler handler;

    Route(String pathSpec, boolean caseInsensitiveMatch, Handler handler) {
        this.pathSpec = new PathSpec(pathSpec, caseInsensitiveMatch);
        this.handler = handler;
    }

    public PathSpec getPathSpec() {
        return pathSpec;
    }

    public Handler getHandler() {
        return handler;
    }
}
