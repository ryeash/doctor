package vest.doctor.ssf.router;

import vest.doctor.ssf.Handler;
import vest.doctor.ssf.RequestContext;

import java.util.Objects;

public class RoutedHandler extends Routed implements Handler {
    private final Handler handler;

    public RoutedHandler(String method, String uriTemplate, Handler handler) {
        super(method, new PathSpec(Objects.requireNonNull(uriTemplate), true));
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void handle(RequestContext ctx) {
        handler.handle(ctx);
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + handler;
    }
}
