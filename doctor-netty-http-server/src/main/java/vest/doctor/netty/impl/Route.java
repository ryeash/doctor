package vest.doctor.netty.impl;

import vest.doctor.netty.Handler;

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
