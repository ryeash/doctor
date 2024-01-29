package vest.doctor.ssf;

public interface FilterChain {

    void next(RequestContext ctx);
}
