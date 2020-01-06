package vest.doctor.netty;

import vest.doctor.Prioritized;

public interface BodyWriter extends Prioritized {

    boolean handles(RequestContext ctx, Object response);

    void write(RequestContext ctx, Object response);
}
