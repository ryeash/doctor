package vest.doctor.ssf.router;

import vest.doctor.ssf.Handler;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.Response;

import java.util.Objects;
import java.util.concurrent.Flow;

public class RoutedHandler extends Routed implements Handler {
    private final Handler handler;

    public RoutedHandler(String method, String uriTemplate, Handler handler) {
        super(method, new PathSpec(Objects.requireNonNull(uriTemplate), true));
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Flow.Publisher<Response> handle(Request request) {
        return handler.handle(request);
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + handler;
    }
}
