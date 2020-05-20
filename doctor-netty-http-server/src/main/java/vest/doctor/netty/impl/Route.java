package vest.doctor.netty.impl;

import vest.doctor.netty.Handler;

public class Route {
    private final PathSpec pathSpec;
    private final Handler handler;

    public Route(String pathSpec, Handler handler) {
        this.pathSpec = new PathSpec(pathSpec);
        this.handler = handler;
    }

    public PathSpec getPathSpec() {
        return pathSpec;
    }

    public Handler getHandler() {
        return handler;
    }
}
