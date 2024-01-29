package vest.doctor.ssf;

import com.sun.net.httpserver.Request;
import vest.doctor.rx.FlowBuilder;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public interface Filter {

    void filter(RequestContext ctx, FilterChain chain);

    static Filter before(UnaryOperator<RequestContext> before) {
        return (request, chain) -> chain.next(before.apply(request));
    }

    static Filter after(UnaryOperator<RequestContext> after) {
        return (ctx, chain) -> {
            chain.next(ctx);
            after.apply(ctx);
        };
    }
}
