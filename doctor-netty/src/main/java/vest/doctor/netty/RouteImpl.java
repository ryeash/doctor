package vest.doctor.netty;

import vest.doctor.DoctorProvider;

public final class RouteImpl<P, T> implements Route<T> {

    private final DoctorProvider<P> provider;
    private final PathSpec pathSpec;
    private final String info;
    private final RouteFunction<P, T> function;

    public RouteImpl(DoctorProvider<P> provider, String method, String pathSpec, String info, RouteFunction<P, T> function) {
        this.provider = provider;
        this.pathSpec = new PathSpec(method, pathSpec);
        this.info = info;
        this.function = function;
    }

    @Override
    public PathSpec pathSpec() {
        return pathSpec;
    }

    @Override
    public T executeRoute(RequestContext ctx, BodyInterchange bodyInterchange) throws Exception {
        return function.apply(provider.get(), ctx, bodyInterchange);
    }

    @Override
    public String toString() {
        return pathSpec + ": " + info;
    }

    @FunctionalInterface
    public interface RouteFunction<P, T> {
        T apply(P instance, RequestContext ctx, BodyInterchange bodyInterchange) throws Exception;
    }
}
