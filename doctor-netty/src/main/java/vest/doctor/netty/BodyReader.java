package vest.doctor.netty;

import vest.doctor.Prioritized;

public interface BodyReader extends Prioritized {

    boolean handles(RequestContext ctx, Class<?> rawType, Class<?>... genericTypes);

    <T> T read(RequestContext ctx, Class<T> rawType, Class<?>... genericTypes);
}
