package vest.doctor.apache.server;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import vest.doctor.apache.server.impl.MethodPredicate;
import vest.doctor.apache.server.impl.MultiPredicate;
import vest.doctor.apache.server.impl.PathPredicate;
import vest.doctor.apache.server.impl.RequestContextWrapper;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface Filter {

    static Filter matching(String methodMatch, String pathSpec, Filter filter) {
        return new PredicatedFilter(new MultiPredicate(new MethodPredicate(methodMatch), new PathPredicate(pathSpec, true)), filter);
    }

    static Filter matching(String pathSpec, Filter filter) {
        return new PredicatedFilter(new PathPredicate(pathSpec, true), filter);
    }

    static Filter matching(RoutePredicate predicate, Filter filter) {
        return new PredicatedFilter(predicate, filter);
    }

    static Filter before(UnaryOperator<RequestContext> filter) {
        return (ctx, chain) -> chain.proceed(filter.apply(ctx));
    }

    static Filter after(BiFunction<RequestContext, AsyncResponseBuilder, AsyncResponseBuilder> filter) {
        return (ctx, chain) -> chain.proceed(new SendInterceptRequestContext(ctx, filter));
    }

    void handle(RequestContext ctx, FilterChain chain) throws HttpException, IOException;

    @FunctionalInterface
    interface FilterChain {
        void proceed(RequestContext ctx) throws HttpException, IOException;
    }

    record PredicatedFilter(RoutePredicate predicate, Filter filter) implements Filter {
        @Override
        public void handle(RequestContext ctx, FilterChain chain) throws HttpException, IOException {
            if (predicate.test(ctx)) {
                filter.handle(ctx, chain);
            } else {
                chain.proceed(ctx);
            }
        }
    }

    final class SendInterceptRequestContext extends RequestContextWrapper {
        private final BiFunction<RequestContext, AsyncResponseBuilder, AsyncResponseBuilder> filter;

        public SendInterceptRequestContext(RequestContext requestContext, BiFunction<RequestContext, AsyncResponseBuilder, AsyncResponseBuilder> filter) {
            super(requestContext);
            this.filter = filter;
        }

        @Override
        public void send(AsyncResponseBuilder response) throws HttpException, IOException {
            super.send(filter.apply(delegate, response));
        }
    }
}
