package vest.doctor.workflow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

public interface Source<T> extends Flow.Subscription, Flow.Processor<T, T> {

    void startSubscription();

    void executorService(ExecutorService executorService);
}
