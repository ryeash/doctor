package vest.doctor.flow;

import java.util.Optional;
import java.util.concurrent.Flow;

public interface Signal<I, O> {
    enum Type {
        ITEM, ERROR, COMPLETE
    }

    Type type();

    I item();

    Throwable error();

    Flow.Subscriber<? super O> subscriber();

    default boolean complete() {
        return type() == Type.COMPLETE;
    }

    default void emit(O out) {
        Optional.ofNullable(subscriber())
                .ifPresent(s -> s.onNext(out));
    }

    default void defaultAction() {
        switch (type()) {
            case ITEM -> throw new UnsupportedOperationException("no default action for item signals");
            case ERROR -> Optional.ofNullable(subscriber()).ifPresent(s -> s.onError(error()));
            case COMPLETE -> Optional.ofNullable(subscriber()).ifPresent(Flow.Subscriber::onComplete);
        }
    }
}
