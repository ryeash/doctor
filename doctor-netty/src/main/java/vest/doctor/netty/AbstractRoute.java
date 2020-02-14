package vest.doctor.netty;

public abstract class AbstractRoute<T> implements Route<T> {

    private final PathSpec pathSpec;

    public AbstractRoute(String method, String pathSpec) {
        this.pathSpec = new PathSpec(method, pathSpec);
    }

    @Override
    public PathSpec pathSpec() {
        return pathSpec;
    }

}
