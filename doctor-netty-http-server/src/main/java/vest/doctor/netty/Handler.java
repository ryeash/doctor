package vest.doctor.netty;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface Handler {

    CompletionStage<Response> handle(Request request);

    static Handler sync(SynchronousHandler function) {
        return function;
    }
}
