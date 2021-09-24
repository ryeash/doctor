package vest.doctor.workflow;

import java.util.Optional;
import java.util.concurrent.Flow;

public interface Signal<IN, OUT> {

    enum Type {
        SUBSCRIBED, VALUE, ERROR, COMPLETED, CANCELED
    }

    IN value();

    Type type();

    Throwable error();

    Flow.Subscription subscription();

    Optional<Flow.Subscriber<OUT>> downstream();
}
