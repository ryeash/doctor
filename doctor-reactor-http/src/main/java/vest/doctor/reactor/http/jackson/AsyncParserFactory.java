package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.databind.JavaType;
import vest.doctor.Prioritized;
import vest.doctor.http.server.RequestContext;

/**
 * A factory that can create new {@link AsyncParser AsyncParsers} for specific requested types.
 */
public interface AsyncParserFactory extends Prioritized {

    /**
     * Create a new parser for the given type.
     *
     * @param requestContext the {@link RequestContext}
     * @param javaType       the type to parse
     * @return a new async parser or null if this factory does not support the type/request
     */
    AsyncParser<?> build(RequestContext requestContext, JavaType javaType);
}
