package vest.doctor.ssf;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Filter {

    CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain);

    static Filter before(Consumer<RequestContext> action) {
        return (ctx, chain) -> {
            action.accept(ctx);
            return chain.next(ctx);
        };
    }

    static Filter after(Consumer<RequestContext> action) {
        return (ctx, chain) ->
                chain.next(ctx)
                        .thenApply(r -> {
                            action.accept(r);
                            return r;
                        });
    }
}
