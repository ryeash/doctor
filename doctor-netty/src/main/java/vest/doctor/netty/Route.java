package vest.doctor.netty;

public interface Route<T> {

    PathSpec pathSpec();

    T executeRoute(RequestContext ctx, BodyInterchange bodyInterchange) throws Exception;
}
