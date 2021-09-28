package vest.doctor.workflow;

import java.util.Optional;
import java.util.concurrent.Flow;

public interface Signal<IN, OUT> {

    enum Type {
        SUBSCRIBED, VALUE, ERROR, COMPLETED, REQUESTED, CANCELED
    }

    IN value();

    Type type();

    Throwable error();

    Flow.Subscription subscription();

    long requested();

    Optional<Flow.Subscriber<OUT>> downstream();

    void doDefaultAction();
}
